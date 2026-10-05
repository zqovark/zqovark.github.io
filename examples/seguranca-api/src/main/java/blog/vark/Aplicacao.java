package blog.vark;

import java.security.Principal;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class Aplicacao {
    public static void main(String[] args) { SpringApplication.run(Aplicacao.class, args); }

    @RestController
    static class Contas {
        @GetMapping({"/api/conta", "/web/conta"})
        Map<String, String> conta(Principal principal) { return Map.of("usuario", principal.getName()); }

        @PostMapping("/web/preferencias")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void preferencias() { /* Exemplo sem persistência. */ }
    }
}
