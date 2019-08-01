package com.monopay.wallet.resolver;

import com.monopay.wallet.entity.Authentication;
import com.monopay.wallet.exception.AuthenticationException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthenticationArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Authentication.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    String merchantId = webRequest.getHeader("X-MERCHANT-ID");
    if (merchantId == null) {
      throw new AuthenticationException();
    } else {
      return Authentication.builder()
        .id(merchantId)
        .build();
    }
  }
}
