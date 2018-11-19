package fishing.lee.backend;

import de.mkammerer.argon2.Argon2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class OrderController {
    private final Argon2 argon;

    @Autowired
    public OrderController(Argon2 argon) {
        this.argon = argon;
    }

    @GetMapping("/order/{id}")
    String one(@PathVariable UUID id) {
        return "whatever";
    }

    @PostMapping("/order")
    String post() {
        return argon.hash(75, 65536, Math.min(Runtime.getRuntime().availableProcessors() / 2, 1), "flibble");
    }
}
