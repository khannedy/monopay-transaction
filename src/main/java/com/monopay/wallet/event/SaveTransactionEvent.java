package com.monopay.wallet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaveTransactionEvent {

  private String id;

  private String merchantId;

  private String balanceId;

  private Long beforeBalance;

  private Long afterBalance;

  private Long beforePoint;

  private Long afterPoint;

  private String type;

  private String bank;

  private String bankAccountNumber;

  private Long createdAt;

  private Long lastModifiedAt;

}
