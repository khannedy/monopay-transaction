package com.monopay.wallet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaveMemberEvent {

  private String id;

  private String merchantId;

  private String name;

  private String email;

  private String phone;

  private Boolean verified;
}
