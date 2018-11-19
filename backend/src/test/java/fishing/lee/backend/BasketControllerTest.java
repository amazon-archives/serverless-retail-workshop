package fishing.lee.backend;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.UUID;

public class BasketControllerTest extends BaseDynamoDBTest {
    @Autowired
    BasketController basketController;

    @Test(expected = BasketNotFoundException.class)
    public void oneUnknown() {
        basketController.one(UUID.randomUUID());
    }

    private BasketModel getMockedBasketModel(String singleItem) {
        BasketModel basketModel = new BasketModel();
        basketModel.setItems(new HashSet<>());
        basketModel.setCreationPoint(null);
        basketModel.addItem(singleItem);
        return basketModel;
    }

    @Test
    public void newBasket() {
        BasketModel basketModel = getMockedBasketModel("test");

        BasketModel result = basketController.newBasket(basketModel);
        assert(result.getItems().contains("test"));
        assert(result.getId() != null);
        assert(result.getCreationPoint() != null);

        BasketModel storedResult = basketController.one(result.getId());
        assert(result.getId().equals(storedResult.getId()));
    }

    @Test
    public void addItem() {
        BasketModel basketModel = getMockedBasketModel("dummy");

        BasketModel result = basketController.newBasket(basketModel);
        basketController.addItem(result.getId(), "item");

        BasketModel storedResult = basketController.one(result.getId());
        assert(storedResult.getItems().contains("item"));
    }

    @Test(expected = BasketNotFoundException.class)
    public void deleteBasket() {
        BasketModel basketModel = getMockedBasketModel("dummy");

        BasketModel result = basketController.newBasket(basketModel);
        basketController.deleteBasket(result.getId());

        basketController.one(result.getId());
    }

    @Test
    public void deleteItem() {
        BasketModel basketModel = getMockedBasketModel("dummy");
        basketModel.addItem("deletedItem");

        BasketModel result = basketController.newBasket(basketModel);
        result = basketController.deleteItem(result.getId(), "deletedItem");
        assert(!result.getItems().contains("deletedItem"));

        BasketModel storedResult = basketController.one(result.getId());
        assert(!storedResult.getItems().contains("deletedItem"));
    }
}