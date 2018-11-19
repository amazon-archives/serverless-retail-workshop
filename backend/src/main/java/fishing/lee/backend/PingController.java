package fishing.lee.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
    @GetMapping("/")
    String pingRoot() {
        return "PONG (from root)";
    }

    @GetMapping("/ping")
    String ping() {
        return "PONG";
    }
}
