/*
 * Copyright Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Red Hat trademarks are not licensed under GPLv3. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.subscriptions.conduit.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.candlepin.subscriptions.conduit.InventoryController;
import org.candlepin.subscriptions.conduit.job.OrgSyncTaskManager;
import org.candlepin.subscriptions.db.OrgConfigRepository;
import org.candlepin.subscriptions.db.model.config.OrgConfig;
import org.candlepin.subscriptions.utilization.api.model.OrgInventory;
import org.candlepin.subscriptions.utilization.api.model.OrgSyncRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;

@SpringBootTest
@WebAppConfiguration
@ActiveProfiles({"rhsm-conduit", "test"})
class InternalOrganizationSyncResourceTest {

  @MockBean InventoryController controller;
  @MockBean OrgConfigRepository repo;
  @MockBean OrgSyncTaskManager tasks;
  @Captor ArgumentCaptor<List<OrgConfig>> orgConfigCaptor;

  @Autowired InternalOrganizationSyncResource resource;

  @Test
  void syncOrgShouldReturnSuccess() {
    var request = new OrgSyncRequest();
    request.setOrgId("123");
    assertEquals("Success", resource.syncOrg(request).getStatus());
  }

  @Test
  void syncAllOrgsShouldReturnSuccess() {
    assertEquals("Success", resource.syncFullOrgList().getStatus());
  }

  @Test
  void hasOrgInSyncListShouldReturnTrue() {
    when(repo.existsById("123")).thenReturn(true);
    assertTrue(
        "Org ID expected to exist but does not",
        resource.hasOrgInSyncList("123").getExistsInList());
  }

  @Test
  void hasOrgInSyncListShouldReturnFalse() {
    when(repo.existsById("123")).thenReturn(false);
    assertFalse(
        "Org ID expected to not exist but does",
        resource.hasOrgInSyncList("123").getExistsInList());
  }

  @Test
  void addOrgsToSyncListShouldReturnSuccess() {
    var orgIds = new ArrayList<>(Arrays.asList("123", "456"));
    assertEquals("Success", resource.addOrgsToSyncList(orgIds).getStatus());
    verify(repo, times(1)).saveAll(orgConfigCaptor.capture());
    assertEquals(2, orgConfigCaptor.getValue().size());
  }

  @Test
  void getInventoryForOrg() {
    String orgId = "123";
    Integer offset = 0;
    OrgInventory expected = new OrgInventory();
    when(controller.getInventoryForOrg(orgId, offset.toString())).thenReturn(expected);

    OrgInventory actual = resource.getInventoryForOrg(orgId, null, offset);
    verify(controller).getInventoryForOrg(orgId, offset.toString());
    assertEquals(expected, actual);
  }
}
