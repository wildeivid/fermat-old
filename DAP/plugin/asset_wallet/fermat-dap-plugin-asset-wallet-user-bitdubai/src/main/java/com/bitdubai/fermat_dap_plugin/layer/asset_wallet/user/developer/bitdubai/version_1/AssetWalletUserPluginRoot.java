package com.bitdubai.fermat_dap_plugin.layer.asset_wallet.user.developer.bitdubai.version_1;

import com.bitdubai.fermat_api.Plugin;

import java.util.UUID;

/**
 * Created by Franklin on 07/09/15.
 */
public class AssetWalletUserPluginRoot implements Plugin {

    UUID pluginId;

    @Override
    public void setId(UUID pluginId) {
        this.pluginId = pluginId;
    }

}