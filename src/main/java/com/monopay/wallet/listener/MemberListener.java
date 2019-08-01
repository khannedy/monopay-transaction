package com.monopay.wallet.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monopay.wallet.entity.Balance;
import com.monopay.wallet.event.SaveMemberEvent;
import com.monopay.wallet.repository.BalanceRepository;
import com.monopay.wallet.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class MemberListener {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private BalanceRepository balanceRepository;

  @Autowired
  private TransactionService transactionService;

  @KafkaListener(topics = "monopay-save-member-event")
  public void onSaveMemberEvent(String payload) throws IOException {
    log.info(payload);
    SaveMemberEvent event = objectMapper.readValue(payload, SaveMemberEvent.class);

    if (!balanceRepository.existsById(event.getId())) {
      Balance balance = Balance.builder()
        .id(event.getId())
        .merchantId(event.getMerchantId())
        .balance(0L)
        .point(0L)
        .build();

      balance = balanceRepository.save(balance);
      transactionService.publishBalance(balance);
    }
  }
}
