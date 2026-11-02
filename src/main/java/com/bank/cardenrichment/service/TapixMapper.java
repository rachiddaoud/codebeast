package com.bank.cardenrichment.service;

import com.bank.cardenrichment.api.model.Address;
import com.bank.cardenrichment.api.model.CardTransaction;
import com.bank.cardenrichment.api.model.Category;
import com.bank.cardenrichment.api.model.Coordinates;
import com.bank.cardenrichment.api.model.EnrichedCardTransaction;
import com.bank.cardenrichment.api.model.Merchant;
import com.bank.cardenrichment.api.model.Shop;
import com.bank.cardenrichment.tapix.model.FindResultComplete;
import com.bank.cardenrichment.tapix.model.Location;
import com.bank.cardenrichment.tapix.model.RequestTX;
import com.bank.cardenrichment.tapix.model.ShopComplete;
import org.springframework.stereotype.Component;

/** Translates between our contract and the Tapix contract. */
@Component
public class TapixMapper {

    public RequestTX toTapixRequest(CardTransaction tx) {
        return RequestTX.builder()
                .customId(tx.getTransactionId())
                .description(tx.getAcceptorName())
                .merchantId(tx.getMerchantId())
                .posId(tx.getTerminalId())
                .city(tx.getCity())
                .country(tx.getCountry())
                .zip(tx.getZip())
                .mcc(tx.getMcc())
                .transactionTimestamp(tx.getTransactionTimestamp())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .ecommerceFlag(tx.getEcommerce())
                .channelCode(tx.getChannel() == null ? null : tx.getChannel().getValue())
                .sourceId(tx.getCardToken())
                .clientId(tx.getClientToken())
                .refresh(false)
                .build();
    }

    public EnrichedCardTransaction toEnriched(String transactionId, FindResultComplete result) {
        boolean found = result.getResult() == FindResultComplete.ResultEnum.FOUND && result.getShop() != null;
        if (!found) {
            return EnrichedCardTransaction.builder()
                    .transactionId(transactionId)
                    .status(EnrichedCardTransaction.StatusEnum.UNSOLVED)
                    .tapixHandle(result.getHandle())
                    .build();
        }
        ShopComplete shop = result.getShop();
        return EnrichedCardTransaction.builder()
                .transactionId(transactionId)
                .status(EnrichedCardTransaction.StatusEnum.FOUND)
                .tapixHandle(result.getHandle())
                .merchant(toMerchant(shop.getMerchant()))
                .shop(toShop(shop))
                .build();
    }

    private Merchant toMerchant(com.bank.cardenrichment.tapix.model.Merchant merchant) {
        if (merchant == null) {
            return null;
        }
        return Merchant.builder()
                .id(merchant.getUid())
                .name(merchant.getName())
                .logoUrl(merchant.getLogo())
                .build();
    }

    private Shop toShop(ShopComplete shop) {
        Location location = shop.getLocation();
        return Shop.builder()
                .id(shop.getUid())
                .type(toShopType(shop.getType()))
                .category(toCategory(shop.getCategory()))
                .tags(shop.getTags())
                .address(location == null ? null : toAddress(location.getAddress()))
                .coordinates(location == null ? null : toCoordinates(location.getCoordinates()))
                .websiteUrl(shop.getUrl())
                .googlePlaceId(shop.getGooglePlaceId())
                .phoneNumber(shop.getPhoneNumber())
                .build();
    }

    private Shop.TypeEnum toShopType(ShopComplete.TypeEnum type) {
        return switch (type) {
            case BRICKS -> Shop.TypeEnum.BRICKS;
            case ONLINE -> Shop.TypeEnum.ONLINE;
            case ATM -> Shop.TypeEnum.ATM;
            case ACCOUNT -> Shop.TypeEnum.ACCOUNT;
            case null, default -> null;
        };
    }

    private Category toCategory(com.bank.cardenrichment.tapix.model.Category category) {
        if (category == null) {
            return null;
        }
        return Category.builder()
                .name(category.getName())
                .logoUrl(category.getLogo())
                .build();
    }

    private Address toAddress(com.bank.cardenrichment.tapix.model.Address address) {
        if (address == null) {
            return null;
        }
        return Address.builder()
                .street(address.getStreet())
                .city(address.getCity())
                .zip(address.getZip())
                .country(address.getCountry())
                .build();
    }

    private Coordinates toCoordinates(com.bank.cardenrichment.tapix.model.Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        return Coordinates.builder()
                .latitude(coordinates.getLat())
                .longitude(coordinates.getLong())
                .build();
    }
}
