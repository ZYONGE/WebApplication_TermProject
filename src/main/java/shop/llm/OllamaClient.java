package shop.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/*
  Ollama /api/generate 호출 지점. 외부 호출은 이 클래스만 한다.

  수업 실습 코드(my.ai.OllamaClient)를 옮겨 왔다. 바꾼 점은 세 가지다.
    1. 주소 · 모델을 소스에 적지 않고 context.xml 의 <Environment> 에서 읽는다.
    2. 오류 응답 본문은 사용자 화면이 아니라 서버 로그로 보낸다 — LlmException 메시지는 화면용이다.
    3. JNDI 조회는 DBUtil 처럼 최초 1회만 한다. 다만 설정이 없어도 앱 전체를 멈추지 않고,
       LLM 을 부르는 순간에만 실패한다.
*/
public class OllamaClient {

	private static final String ENV_URL = "java:comp/env/llm/ollamaUrl";
	private static final String ENV_MODEL = "java:comp/env/llm/ollamaModel";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(180);

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.build();

	private static String apiUrl;
	private static String model;

	static {
		try {
			Context init = new InitialContext();
			apiUrl = (String) init.lookup(ENV_URL);
			model = (String) init.lookup(ENV_MODEL);
		} catch (NamingException e) {
			/* 설정이 없으면 null 로 두고 generate() 에서 안내한다. */
		}
	}

	/*
	  프롬프트 하나를 보내고 답변 문자열을 받는다. stream=false 라 한 번에 온다.

	  InterruptedException 은 감싸지 않고 그대로 던진다 — 호출한 쪽이
	  인터럽트 상태를 복원해야 하기 때문이다.
	*/
	public String generate(String prompt) throws LlmException, InterruptedException {
		if (apiUrl == null || model == null) {
			throw new LlmException("AI 설정이 없습니다. context.xml 의 llm/ollamaUrl · llm/ollamaModel 을 확인하세요.");
		}

		JsonObject body = new JsonObject();
		body.addProperty("model", model);
		body.addProperty("prompt", prompt);
		body.addProperty("stream", false);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl))
				.timeout(REQUEST_TIMEOUT)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> response;
		try {
			response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new LlmException("AI 서버에 연결하지 못했습니다.", e);
		}

		if (response.statusCode() != 200) {
			throw new LlmException("AI 서버 오류입니다. (HTTP " + response.statusCode() + ")",
					new IOException(response.body()));
		}

		try {
			JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
			JsonElement answer = result.get("response");
			if (answer == null || answer.isJsonNull()) {
				throw new LlmException("AI 응답에 답변이 없습니다.");
			}
			return answer.getAsString();
		} catch (JsonParseException | IllegalStateException e) {
			throw new LlmException("AI 응답을 해석하지 못했습니다.", e);
		}
	}
}
