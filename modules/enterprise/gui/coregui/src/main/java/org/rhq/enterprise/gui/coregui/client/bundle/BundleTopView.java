/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.bundle;

import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.enterprise.gui.coregui.client.bundle.create.BundleCreationWizard;

/**
 * @author Greg Hinkle
 */
public class BundleTopView extends HLayout {

    @Override
    protected void onDraw() {
        super.onDraw();

        IButton addButton = new IButton("New Bundle");
        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new BundleCreationWizard().startBundleWizard();
            }
        });

        addMember(addButton);
    }
}
