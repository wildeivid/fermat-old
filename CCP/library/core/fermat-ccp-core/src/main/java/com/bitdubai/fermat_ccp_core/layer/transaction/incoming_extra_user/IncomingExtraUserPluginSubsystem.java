package com.bitdubai.fermat_ccp_core.layer.transaction.incoming_extra_user;

import com.bitdubai.fermat_api.layer.all_definition.common.abstract_classes.AbstractPluginSubsystem;
import com.bitdubai.fermat_api.layer.all_definition.common.exceptions.CantStartSubsystemException;
import com.bitdubai.fermat_api.layer.all_definition.common.utils.PluginReference;
import com.bitdubai.fermat_ccp_api.all_definition.enums.CCPPlugins;
import com.bitdubai.fermat_ccp_plugin.layer.transaction.incoming_extra_user.developer.bitdubai.DeveloperBitDubai;

/**
 * Created by Leon Acosta - (laion.cj91@gmail.com) on 21/10/2015.
 *
 * @version 1.0
 * @since Java JDK 1.7
 */
public class IncomingExtraUserPluginSubsystem extends AbstractPluginSubsystem {

    public IncomingExtraUserPluginSubsystem() {
        super(new PluginReference(CCPPlugins.BITDUBAI_INCOMING_EXTRA_USER_TRANSACTION));
    }

    @Override
    public void start() throws CantStartSubsystemException {
        /**
         * I will choose from the different Developers available which implementation to use. Right now there is only
         * one, so it is not difficult to choose.
         */

        try {
            DeveloperBitDubai developerBitDubai = new DeveloperBitDubai();
            plugin = developerBitDubai.getPlugin();
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            throw new CantStartSubsystemException(e, null, null);
        }
    }

}