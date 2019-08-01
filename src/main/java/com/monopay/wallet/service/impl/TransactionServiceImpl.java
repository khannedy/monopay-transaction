package com.monopay.wallet.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monopay.wallet.entity.*;
import com.monopay.wallet.event.SaveTransactionEvent;
import com.monopay.wallet.model.service.*;
import com.monopay.wallet.model.web.response.*;
import com.monopay.wallet.repository.BalanceRepository;
import com.monopay.wallet.repository.TransactionRepository;
import com.monopay.wallet.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
public class TransactionServiceImpl implements TransactionService {

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private BalanceRepository balanceRepository;

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public TopUpTransactionWebResponse topUp(@Valid TopUpTransactionServiceRequest request) {
    Balance balance = balanceRepository.findById(request.getMemberId()).get();
    balance.setBalance(balance.getBalance() + request.getTotal());
    balance = balanceRepository.save(balance);
    publishBalance(balance);

    Transaction transaction = Transaction.builder()
      .id(UUID.randomUUID().toString())
      .beforeBalance(balance.getBalance() - request.getTotal())
      .afterBalance(balance.getBalance())
      .beforePoint(balance.getPoint())
      .afterPoint(balance.getPoint())
      .type(TransactionType.TOPUP)
      .balanceId(request.getMemberId())
      .merchantId(request.getMerchantId())
      .build();
    transaction = transactionRepository.save(transaction);
    publishTransaction(toSaveTransactionEvent(transaction));

    return TopUpTransactionWebResponse.builder()
      .memberId(request.getMemberId())
      .beforeBalance(transaction.getBeforeBalance())
      .afterBalance(transaction.getAfterBalance())
      .beforePoint(transaction.getBeforePoint())
      .afterPoint(transaction.getAfterPoint())
      .type(transaction.getType())
      .build();
  }

  @Override
  public PurchaseTransactionWebResponse purchase(@Valid PurchaseTransactionServiceRequest request) {
    Transaction transaction = Transaction.builder()
      .id(UUID.randomUUID().toString())
      .type(TransactionType.PURCHASE)
      .balanceId(request.getMemberId())
      .merchantId(request.getMerchantId())
      .build();

    Balance balance = balanceRepository.findById(request.getMemberId()).get();

    if (balance.getPoint() > request.getTotal()) {
      balance.setPoint(balance.getPoint() - request.getTotal());
      transaction.setBeforePoint(balance.getPoint() + request.getTotal());
      transaction.setAfterPoint(balance.getPoint());
      transaction.setBeforeBalance(balance.getBalance());
      transaction.setAfterBalance(balance.getBalance());

    } else {
      Long leftovers = request.getTotal() - balance.getPoint();

      transaction.setBeforePoint(balance.getPoint());
      transaction.setAfterPoint(0L);
      balance.setPoint(0L);

      balance.setBalance(balance.getBalance() - leftovers);
      transaction.setBeforeBalance(balance.getBalance() + leftovers);
      transaction.setAfterBalance(balance.getBalance());
    }

    balance = balanceRepository.save(balance);
    publishBalance(balance);

    transaction = transactionRepository.save(transaction);
    publishTransaction(toSaveTransactionEvent(transaction));

    return PurchaseTransactionWebResponse.builder()
      .memberId(request.getMemberId())
      .beforeBalance(transaction.getBeforeBalance())
      .afterBalance(transaction.getAfterBalance())
      .beforePoint(transaction.getBeforePoint())
      .afterPoint(transaction.getAfterPoint())
      .type(transaction.getType())
      .build();
  }

  @Override
  public PointTransactionWebResponse point(@Valid PointTransactionServiceRequest request) {
    Balance balance = balanceRepository.findById(request.getMemberId()).get();
    balance.setPoint(balance.getPoint() + request.getTotal());
    balance = balanceRepository.save(balance);
    publishBalance(balance);

    Transaction transaction = Transaction.builder()
      .id(UUID.randomUUID().toString())
      .beforeBalance(balance.getBalance())
      .afterBalance(balance.getBalance())
      .beforePoint(balance.getPoint() - request.getTotal())
      .afterPoint(balance.getPoint())
      .type(TransactionType.POINT)
      .balanceId(request.getMemberId())
      .merchantId(request.getMerchantId())
      .build();
    transaction = transactionRepository.save(transaction);
    publishTransaction(toSaveTransactionEvent(transaction));

    return PointTransactionWebResponse.builder()
      .memberId(request.getMemberId())
      .beforeBalance(transaction.getBeforeBalance())
      .afterBalance(transaction.getAfterBalance())
      .beforePoint(transaction.getBeforePoint())
      .afterPoint(transaction.getAfterPoint())
      .type(transaction.getType())
      .build();
  }

  @Override
  public TransferTransactionWebResponse transfer(@Valid TransferTransactionServiceRequest request) {
    Balance balance = balanceRepository.findById(request.getMemberId()).get();
    balance.setBalance(balance.getBalance() - request.getTotal());
    balance = balanceRepository.save(balance);
    publishBalance(balance);

    Transaction transaction = Transaction.builder()
      .id(UUID.randomUUID().toString())
      .beforeBalance(balance.getBalance() + request.getTotal())
      .afterBalance(balance.getBalance())
      .beforePoint(balance.getPoint())
      .afterPoint(balance.getPoint())
      .type(TransactionType.TRANSFER)
      .balanceId(request.getMemberId())
      .merchantId(request.getMerchantId())
      .build();
    transaction = transactionRepository.save(transaction);

    SaveTransactionEvent event = toSaveTransactionEvent(transaction);
    event.setBank(request.getBank());
    event.setBankAccountNumber(request.getBankAccountNumber());
    publishTransaction(event);

    return TransferTransactionWebResponse.builder()
      .memberId(request.getMemberId())
      .beforeBalance(transaction.getBeforeBalance())
      .afterBalance(transaction.getAfterBalance())
      .beforePoint(transaction.getBeforePoint())
      .afterPoint(transaction.getAfterPoint())
      .type(transaction.getType())
      .build();
  }

  @Override
  public List<TransactionWebResponse> list(@Valid ListTransactionServiceRequest request) {
    return transactionRepository.findAllByBalanceIdAndMerchantIdOrderByCreatedAtDesc(request.getMemberId(), request.getMerchantId())
      .stream()
      .map(transaction -> TransactionWebResponse.builder()
        .memberId(request.getMemberId())
        .beforeBalance(transaction.getBeforeBalance())
        .afterBalance(transaction.getAfterBalance())
        .beforePoint(transaction.getBeforePoint())
        .afterPoint(transaction.getAfterPoint())
        .type(transaction.getType())
        .createdAt(transaction.getCreatedAt())
        .build())
      .collect(Collectors.toList());
  }

  @Override
  public void publishBalance(Balance balance) {
    try {
      String payload = objectMapper.writeValueAsString(balance);
      kafkaTemplate.send("monopay-save-balance-event", payload);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public void publishTransaction(SaveTransactionEvent transaction) {
    try {
      String payload = objectMapper.writeValueAsString(transaction);
      kafkaTemplate.send("monopay-save-transaction-event", payload);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
    }
  }

  private SaveTransactionEvent toSaveTransactionEvent(Transaction transaction) {
    SaveTransactionEvent event = new SaveTransactionEvent();
    BeanUtils.copyProperties(transaction, event);
    event.setType(transaction.getType().toString());
    return event;
  }
}
