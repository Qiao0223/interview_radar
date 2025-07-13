package com.interviewradar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final String COOKIE_VALUE =
            "gr_user_id=ae74305e-23b3-4c0b-b2d1-39b806856783; NOWCODERCLINETID=1C48FBC7A6BB2229CDD8C09365CF62E4; NOWCODERUID=A4B6853031037C00CBAB6B71D7544EA2; __snaker__id=Y6SaTU3qnuxkYVw6; csrfToken=NEHhWJqOdAQxNdP8JcEsipT9; Hm_lvt_a808a1326b6c06c437de769d1b85b870=1748937382,1749115332,1751197986; HMACCOUNT=32D4A73834CADBC2; isAgreementChecked=true; gdxidpyhxdE=x%2BBsYcRcY0JpkgZylKOAChExZ0Qg%2BGTjawzfSdX%2F%5CbS4XZ7WH1Ui2sML5NUAXE1Yzd2aWI8ZI6Axc7vx1G6QpTXQ0RoEl6mIL%2Fqbxfdvf5%5CHvPC1QUvllZdMtbHTkvgj2nI75MItYKGShyOSK1OmJ%2BxPqxAf0CCuEyGwROGo%5CX5gmm7A%3A1751207818743; t=7B3111F02B73AD3AB566D5BB81B024E1; c196c3667d214851b11233f5c17f99d5_gr_last_sent_cs1=964175392; c196c3667d214851b11233f5c17f99d5_gr_session_id=83b2621b-a028-42dd-bc62-387c743683d1; c196c3667d214851b11233f5c17f99d5_gr_last_sent_sid_with_cs1=83b2621b-a028-42dd-bc62-387c743683d1; c196c3667d214851b11233f5c17f99d5_gr_session_id_83b2621b-a028-42dd-bc62-387c743683d1=true; SERVERID=4e3293274e97e39c4e3710f7834dfd16|1751216133|1751213273; SERVERCORSID=4e3293274e97e39c4e3710f7834dfd16|1751216133|1751213273; Hm_lpvt_a808a1326b6c06c437de769d1b85b870=1751216134; c196c3667d214851b11233f5c17f99d5_gr_cs1=964175392";  // 你的完整 Cookie


    @Bean
    public RestTemplate restTemplate() {
        RestTemplate rest = new RestTemplate();
        ClientHttpRequestInterceptor cookieInterceptor = (request, body, execution) -> {
            request.getHeaders().add("Cookie", COOKIE_VALUE);
            return execution.execute(request, body);
        };
        rest.getInterceptors().add(cookieInterceptor);
        return rest;
    }
}

