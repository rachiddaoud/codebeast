package com.bank.cardenrichment.mapper;

import com.bank.cardenrichment.api.model.CardTransaction;
import com.bank.cardenrichment.api.model.EnrichedCardTransaction;
import com.bank.cardenrichment.api.model.Shop;
import com.bank.cardenrichment.tapix.model.FindResultComplete;
import com.bank.cardenrichment.tapix.model.RequestTX;
import com.bank.cardenrichment.tapix.model.ShopComplete;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

/** Translates between our contract and the Tapix contract. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TapixMapper {

    @Mapping(target = "customId", source = "transactionId")
    @Mapping(target = "description", source = "acceptorName")
    @Mapping(target = "posId", source = "terminalId")
    @Mapping(target = "ecommerceFlag", source = "ecommerce")
    @Mapping(target = "channelCode", source = "channel")
    @Mapping(target = "sourceId", source = "cardToken")
    @Mapping(target = "clientId", source = "clientToken")
    @Mapping(target = "refresh", constant = "false")
    RequestTX toTapixRequest(CardTransaction tx);

    @Mapping(target = "transactionId", source = "transactionId")
    @Mapping(target = "status", source = "result.result")
    @Mapping(target = "tapixHandle", source = "result.handle")
    @Mapping(target = "merchant", source = "result.shop.merchant")
    @Mapping(target = "shop", source = "result.shop")
    EnrichedCardTransaction toEnriched(String transactionId, FindResultComplete result);

    @ValueMapping(target = "UNSOLVED", source = MappingConstants.ANY_REMAINING)
    EnrichedCardTransaction.StatusEnum toStatus(FindResultComplete.ResultEnum result);

    @Mapping(target = "id", source = "uid")
    @Mapping(target = "address", source = "location.address")
    @Mapping(target = "coordinates", source = "location.coordinates")
    @Mapping(target = "websiteUrl", source = "url")
    Shop toShop(ShopComplete shop);

    @ValueMapping(target = MappingConstants.NULL, source = MappingConstants.ANY_REMAINING)
    Shop.TypeEnum toShopType(ShopComplete.TypeEnum type);

    @Mapping(target = "id", source = "uid")
    @Mapping(target = "logoUrl", source = "logo")
    com.bank.cardenrichment.api.model.Merchant toMerchant(com.bank.cardenrichment.tapix.model.Merchant merchant);

    @Mapping(target = "logoUrl", source = "logo")
    com.bank.cardenrichment.api.model.Category toCategory(com.bank.cardenrichment.tapix.model.Category category);

    @Mapping(target = "latitude", source = "lat")
    @Mapping(target = "longitude", source = "long")
    com.bank.cardenrichment.api.model.Coordinates toCoordinates(com.bank.cardenrichment.tapix.model.Coordinates coordinates);
}
