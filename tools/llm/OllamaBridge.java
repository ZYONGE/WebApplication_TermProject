import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/*
  Ollama → OpenAI 호환 중계 (점검용, 앱에 포함되지 않는다). docs/tracks/LLM.md § 11

  앱(OllamaClient)은 나중에 연결할 학교 Ollama 서버 형식(/api/generate)으로만 말한다.
  학교 서버를 받기 전까지 기능 점검은 USB 소형 모델(llama.cpp llama-server, OpenAI 호환
  /v1/chat/completions)로 하므로, 그 사이에서 요청 · 응답 형식만 바꿔 준다.

    OllamaClient ──▶ 127.0.0.1:31434 /api/generate ──▶ [이 중계] ──▶ 127.0.0.1:8081 /v1/chat/completions

  실행 (프로젝트 루트에서, 빌드 없음)
    java -cp WebContents/WEB-INF/lib/gson-2.15.0.jar tools/llm/OllamaBridge.java

  환경변수 (기본값)
    BRIDGE_PORT=31434                       context.xml 의 llm/ollamaUrl 포트와 같다
    UPSTREAM=http://127.0.0.1:8081/v1       USB 모델 서버 (Tomcat 과 겹치지 않게 8081)
    UPSTREAM_MODEL=local-test-4b            USB 서버의 --alias

  변환 규칙
    prompt                → messages[{role:user}]
    format: "json"        → response_format {type: json_object}
    temperature 0 · seed 42 · max_tokens 512   USB README § 6 공통 파라미터
    choices[0].message.content → response
  요청의 model 값은 무시하고 UPSTREAM_MODEL 로 바꾼다 (앱은 gemma3:12b 를 보낸다).
*/
public class OllamaBridge {

	private static final int PORT = Integer.parseInt(env("BRIDGE_PORT", "31434"));
	private static final String UPSTREAM = env("UPSTREAM", "http://127.0.0.1:8081/v1");
	private static final String UPSTREAM_MODEL = env("UPSTREAM_MODEL", "local-test-4b");

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	public static void main(String[] args) throws IOException {
		/* 이 PC 에서만 열린다. */
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
		server.createContext("/api/generate", OllamaBridge::generate);
		server.createContext("/api/tags", OllamaBridge::tags);
		server.start();
		System.out.println("[bridge] 127.0.0.1:" + PORT + " /api/generate → " + UPSTREAM + "/chat/completions (" + UPSTREAM_MODEL + ")");
	}

	private static void generate(HttpExchange ex) throws IOException {
		try {
			if (!"POST".equals(ex.getRequestMethod())) {
				send(ex, 405, error("POST 만 받는다"));
				return;
			}
			JsonObject in = JsonParser.parseString(
					new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			JsonElement prompt = in.get("prompt");
			if (prompt == null || prompt.isJsonNull()) {
				send(ex, 400, error("prompt 가 없다"));
				return;
			}

			JsonObject message = new JsonObject();
			message.addProperty("role", "user");
			message.addProperty("content", prompt.getAsString());
			JsonArray messages = new JsonArray();
			messages.add(message);

			JsonObject out = new JsonObject();
			out.addProperty("model", UPSTREAM_MODEL);
			out.add("messages", messages);
			out.addProperty("temperature", 0);
			out.addProperty("seed", 42);
			out.addProperty("max_tokens", 512);
			out.addProperty("stream", false);
			if (in.has("format") && "json".equals(in.get("format").getAsString())) {
				JsonObject format = new JsonObject();
				format.addProperty("type", "json_object");
				out.add("response_format", format);
			}

			long start = System.currentTimeMillis();
			HttpResponse<String> res = HTTP.send(
					HttpRequest.newBuilder()
							.uri(URI.create(UPSTREAM + "/chat/completions"))
							.timeout(Duration.ofSeconds(170))
							.header("Content-Type", "application/json")
							.POST(HttpRequest.BodyPublishers.ofString(out.toString(), StandardCharsets.UTF_8))
							.build(),
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

			/* 상류 오류(컨텍스트 초과 400 등)는 상태 코드를 그대로 돌려준다 — 앱의 오류 처리를 점검하려고. */
			if (res.statusCode() != 200) {
				System.out.println("[bridge] upstream HTTP " + res.statusCode());
				send(ex, res.statusCode(), res.body());
				return;
			}

			String content = JsonParser.parseString(res.body()).getAsJsonObject()
					.getAsJsonArray("choices").get(0).getAsJsonObject()
					.getAsJsonObject("message").get("content").getAsString();

			JsonObject reply = new JsonObject();
			reply.addProperty("model", UPSTREAM_MODEL);
			reply.addProperty("response", content);
			reply.addProperty("done", true);
			System.out.println("[bridge] 200 " + (System.currentTimeMillis() - start) + "ms");
			send(ex, 200, reply.toString());

		} catch (JsonParseException | IllegalStateException | NullPointerException | IndexOutOfBoundsException e) {
			send(ex, 502, error("상류 응답 형식이 다르다: " + e.getMessage()));
		} catch (IOException e) {
			/* USB 모델 서버가 꺼져 있으면 여기로 온다. */
			send(ex, 502, error("USB 모델 서버에 연결하지 못했다: " + UPSTREAM));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			send(ex, 503, error("중단됨"));
		}
	}

	/* 연결 확인용 — Ollama 의 모델 목록 형식으로 흉내 낸다. */
	private static void tags(HttpExchange ex) throws IOException {
		JsonObject model = new JsonObject();
		model.addProperty("name", UPSTREAM_MODEL);
		JsonArray models = new JsonArray();
		models.add(model);
		JsonObject reply = new JsonObject();
		reply.add("models", models);
		send(ex, 200, reply.toString());
	}

	private static String error(String message) {
		JsonObject o = new JsonObject();
		o.addProperty("error", message);
		return o.toString();
	}

	private static void send(HttpExchange ex, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		ex.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static String env(String name, String defaultValue) {
		String v = System.getenv(name);
		return v == null || v.isEmpty() ? defaultValue : v;
	}
}
