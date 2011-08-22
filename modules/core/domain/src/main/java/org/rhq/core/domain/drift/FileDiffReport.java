/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.domain.drift;

import java.io.Serializable;
import java.util.List;

public class FileDiffReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private int changes;

    List<String> diff;

    public FileDiffReport() {
    }

    public FileDiffReport(int numChanges, List<String> diff) {
        changes = numChanges;
        this.diff = diff;
    }

    public int getNumberOfChanges() {
        return changes;
    }

    public List<String> getDiff() {
        return diff;
    }

}