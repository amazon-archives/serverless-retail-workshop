package fishing.lee.backend;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Return a 404 when a basket isn't found.
 *
 * @see ResponseStatus
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "basket not found")
class BasketNotFoundException extends RuntimeException {
}
