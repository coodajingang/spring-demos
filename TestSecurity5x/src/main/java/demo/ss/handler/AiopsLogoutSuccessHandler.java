package demo.ss.handler;

import cn.hutool.json.JSONUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Component
public class AiopsLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        response.setHeader("Content-Type", "application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "logout success");
        result(response, res);
    }

    private void result(HttpServletResponse response, Map<String, Object> result) throws IOException {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.write(JSONUtil.toJsonStr(result));
            writer.flush();
        }finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
