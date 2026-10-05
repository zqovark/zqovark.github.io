package blog.vark;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "demo.password=senha-somente-no-teste")
@AutoConfigureMockMvc
@Import(SegurancaTest.ChavesDeTeste.class)
class SegurancaTest {
    static final KeyPair CHAVE = novaChave();
    @Autowired MockMvc mvc;

    static KeyPair novaChave() {
        try { var gerador = KeyPairGenerator.getInstance("RSA"); gerador.initialize(2048); return gerador.generateKeyPair(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @TestConfiguration
    static class ChavesDeTeste {
        @Bean @Primary
        JwtDecoder decoderLocal() {
            var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) CHAVE.getPublic()).build();
            decoder.setJwtValidator(Seguranca.validacao("https://id.example.invalid", "vark-api"));
            return decoder;
        }
    }

    static String token(KeyPair chave, String issuer, String audience, Instant exp, String scope) throws Exception {
        return token(chave, issuer, audience, exp, scope, "usuario-1");
    }

    static String token(KeyPair chave, String issuer, String audience, Instant exp, String scope, String sub) throws Exception {
        var builder = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(sub)
            .issueTime(Date.from(Instant.now().minusSeconds(900)))
            .claim("scope", scope);
        if (exp != null) builder.expirationTime(Date.from(exp));
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), builder.build());
        jwt.sign(new RSASSASigner(chave.getPrivate()));
        return jwt.serialize();
    }

    String valido(String scope) throws Exception {
        return token(CHAVE, "https://id.example.invalid", "vark-api", Instant.now().plusSeconds(300), scope);
    }

    @Test void semTokenRetorna401() throws Exception {
        mvc.perform(get("/api/conta")).andExpect(status().isUnauthorized());
    }
    @Test void tokenValidoPassaSemCriarSessao() throws Exception {
        var resultado = mvc.perform(get("/api/conta").header("Authorization", "Bearer " + valido("conta:ler")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.usuario").value("usuario-1")).andReturn();
        assertNull(resultado.getRequest().getSession(false));
    }
    @Test void scopeInsuficienteRetorna403() throws Exception {
        mvc.perform(get("/api/conta").header("Authorization", "Bearer " + valido("outra:ler")))
            .andExpect(status().isForbidden());
    }
    @Test void assinaturaDiferenteRetorna401() throws Exception {
        String jwt = token(novaChave(), "https://id.example.invalid", "vark-api", Instant.now().plusSeconds(300), "conta:ler");
        mvc.perform(get("/api/conta").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }
    @Test void tokenExpiradoRetorna401() throws Exception {
        String jwt = token(CHAVE, "https://id.example.invalid", "vark-api", Instant.now().minusSeconds(300), "conta:ler");
        mvc.perform(get("/api/conta").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }
    @Test void issuerDiferenteRetorna401() throws Exception {
        String jwt = token(CHAVE, "https://outro.example.invalid", "vark-api", Instant.now().plusSeconds(300), "conta:ler");
        mvc.perform(get("/api/conta").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }
    @Test void audienceDiferenteRetorna401() throws Exception {
        String jwt = token(CHAVE, "https://id.example.invalid", "outra-api", Instant.now().plusSeconds(300), "conta:ler");
        mvc.perform(get("/api/conta").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }
    @Test void expiracaoAusenteRetorna401() throws Exception {
        String jwt = token(CHAVE, "https://id.example.invalid", "vark-api", null, "conta:ler");
        mvc.perform(get("/api/conta").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }
    @Test void sujeitoVazioRetorna401() throws Exception {
        String jwt = token(CHAVE, "https://id.example.invalid", "vark-api", Instant.now().plusSeconds(300), "conta:ler", "");
        mvc.perform(get("/api/conta").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
    }
    @Test void loginInvalidoNaoAutentica() throws Exception {
        mvc.perform(formLogin().user("neto").password("errada")).andExpect(unauthenticated());
    }
    @Test void loginTrocaIdDaSessao() throws Exception {
        var antes = new MockHttpSession();
        String idAntes = antes.getId();
        var resultado = mvc.perform(post("/login").session(antes).with(csrf())
            .param("username", "neto").param("password", "senha-somente-no-teste"))
            .andExpect(authenticated().withUsername("neto")).andReturn();
        assertNotEquals(idAntes, resultado.getRequest().getSession(false).getId());
    }
    @Test void sessaoExigeCsrfENaoAutenticaApi() throws Exception {
        var resultado = mvc.perform(formLogin().user("neto").password("senha-somente-no-teste"))
            .andExpect(authenticated()).andReturn();
        var sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(get("/web/conta").session(sessao)).andExpect(status().isOk());
        mvc.perform(post("/web/preferencias").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(post("/web/preferencias").session(sessao).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/conta").session(sessao)).andExpect(status().isUnauthorized());
        mvc.perform(post("/logout").session(sessao).with(csrf())).andExpect(unauthenticated());
        assertTrue(sessao.isInvalid());
    }
}
