package fishing.lee.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Baskets, they're a thing... provide an API to them
 */
@RestController
public class BasketController {
    private final BasketRepository basketRepository;

    @Autowired
    public BasketController(BasketRepository basketRepository) {
        this.basketRepository = basketRepository;
    }

    /**
     * Get a single basket
     *
     * @param id UUID of basket to fetch
     * @return Basket instance
     * @throws BasketNotFoundException for returning a 404 via HTTP
     */
    @SuppressWarnings("WeakerAccess")
    @GetMapping("/baskets/{id}")
    BasketModel one(@PathVariable UUID id) throws BasketNotFoundException {
        Optional<BasketModel> basket = basketRepository.findById(id);
        return basket.orElseThrow(BasketNotFoundException::new);
    }

    @PostMapping("/baskets")
    BasketModel newBasket(@RequestBody BasketModel basketModel) {
        // TODO(leepac): This feels suboptimal but the only way to guarantee it's set
        if (Objects.isNull(basketModel.getCreationPoint())) {
            basketModel.setCreationPoint(null);
        }

        return basketRepository.save(basketModel);
    }

    /**
     * Add an item to a basket
     *
     * @param id the ID of the Basket
     * @param item the item to add
     * @return new version of BasketModel
     */
    @PostMapping("/baskets/{id}/add/{item}")
    BasketModel addItem(@PathVariable UUID id, @PathVariable String item) {
        BasketModel basketModel = one(id);
        basketModel.addItem(item);
        basketRepository.save(basketModel);
        return basketModel;
    }

    /**
     * Delete a basket
     *
     * @param id the ID of the basket
     */
    @DeleteMapping("/baskets/{id}")
    void deleteBasket(@PathVariable UUID id) {
        BasketModel basketModel = one(id);
        basketRepository.delete(basketModel);
    }

    /**
     * Delete an item from a basket
     *
     * @param id the ID of the basket
     * @param item the item to remove
     * @return the modified basket
     */
    @DeleteMapping("/baskets/{id}/add/{item}")
    BasketModel deleteItem(@PathVariable UUID id, @SuppressWarnings("SameParameterValue") @PathVariable String item) {
        BasketModel basketModel = one(id);
        basketModel.deleteItem(item);
        basketRepository.save(basketModel);
        return basketModel;
    }
}
