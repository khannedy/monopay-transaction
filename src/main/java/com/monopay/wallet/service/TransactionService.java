package com.monopay.wallet.service;

import com.monopay.wallet.entity.Balance;
import com.monopay.wallet.event.SaveTransactionEvent;
import com.monopay.wallet.model.service.*;
import com.monopay.wallet.model.web.response.*;

import javax.validation.Valid;
import java.util.List;

public interface TransactionService {

  TopUpTransactionWebResponse topUp(@Valid TopUpTransactionServiceRequest request);

  PurchaseTransactionWebResponse purchase(@Valid PurchaseTransactionServiceRequest request);

  PointTransactionWebResponse point(@Valid PointTransactionServiceRequest request);

  TransferTransactionWebResponse transfer(@Valid TransferTransactionServiceRequest request);

  List<TransactionWebResponse> list(@Valid ListTransactionServiceRequest request);

  void publishBalance(Balance balance);

  void publishTransaction(SaveTransactionEvent transaction);
}
