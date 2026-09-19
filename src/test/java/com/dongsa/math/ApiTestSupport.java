package com.dongsa.math;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 테스트에서 HTTP 를 편하게 두드리기 위한 도우미. */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;

    protected MvcResult call(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(req).andReturn();
    }

    protected JsonNode bodyOf(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** 원장 계정을 하나 만들고 토큰을 돌려준다. */
    protected Signup signupOwner(String email, String academyName) throws Exception {
        MvcResult r = call(post("/api/auth/signup"), null, java.util.Map.of(
                "email", email, "password", "password123", "name", "고모", "academyName", academyName));
        JsonNode b = bodyOf(r);
        return new Signup(b.get("token").asText(), b.get("user").get("academyCode").asText(),
                b.get("user").get("id").asLong());
    }

    protected record Signup(String token, String academyCode, long teacherId) {}
}
