package com.projects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

//import com.projects.repo.PaymentsRepository;

import java.util.Map;

@SpringBootApplication
public class BajajApplication implements CommandLineRunner {

//	@Autowired
//	private PaymentsRepository paymentsRepository;
  private final WebClient web;

  public BajajApplication(WebClient.Builder builder) {
    this.web = builder.build();
  }

  public static void main(String[] args) {
    SpringApplication.run(BajajApplication.class, args);
  }

  @Override
  public void run(String... args) {
    // 1) Generate webhook + token
    WebhookResponse resp = web.post()
      .uri("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(Map.of(
        "name",  "Karlakunta Sri Harsha Royal",          // <= put your real name
        "regNo", "22BCE0893",                  // <= your real regNo ending with 93
        "email", "harsharoyal0365@gmail.com"     // <= your real email
      ))
      .retrieve()
      .bodyToMono(WebhookResponse.class)
      .block();

    if (resp == null || resp.getWebhook() == null || resp.getAccessToken() == null) {
      System.err.println("Failed to get webhook or accessToken");
      return;
    }

    // 2) Your final SQL (Question 1)
    String finalQuery =
      "SELECT p.AMOUNT AS SALARY, " +
      "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
      "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
      "d.DEPARTMENT_NAME " +
      "FROM PAYMENTS p " +
      "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
      "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
      "WHERE DAY(p.PAYMENT_TIME) <> 1 " +
      "ORDER BY p.AMOUNT DESC " +
      "LIMIT 1;";

// // Fetch all payments
//    paymentsRepository.findAll().forEach(System.out::println);
    // 3) Submit solution to returned webhook with JWT
    // Some setups expect raw token in Authorization; others expect Bearer <token>.
    // We'll try raw first; if 401, try Bearer.
    String webhookUrl = resp.getWebhook();
    String token = resp.getAccessToken();

    String result = postSolution(webhookUrl, token, finalQuery);
    if (result != null && result.contains("401")) {
      // retry with Bearer
      result = postSolution(webhookUrl, "Bearer " + token, finalQuery);
    }

    System.out.println("Submission response: " + result);
  }

  private String postSolution(String url, String authHeader, String finalQuery) {
    try {
      return web.post()
        .uri(url)
        .header("Authorization", authHeader)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("finalQuery", finalQuery))
        .retrieve()
        .bodyToMono(String.class)
        .block();
    } catch (Exception e) {
      System.err.println("Posting solution failed: " + e.getMessage());
      return "401? " + e.getMessage();
    }
  }
}
