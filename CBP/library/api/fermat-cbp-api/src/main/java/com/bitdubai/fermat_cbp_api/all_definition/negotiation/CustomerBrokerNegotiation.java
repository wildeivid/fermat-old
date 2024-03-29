package com.bitdubai.fermat_cbp_api.all_definition.negotiation;

import com.bitdubai.fermat_cbp_api.all_definition.enums.ClauseStatus;
import com.bitdubai.fermat_cbp_api.all_definition.enums.ClauseType;

import java.util.Collection;

/**
 * Created by jorge on 12-10-2015.
 */
public interface CustomerBrokerNegotiation extends Negotiation {
    String getCustomerPublicKey();
    String getBrokerPublicKey();
}
