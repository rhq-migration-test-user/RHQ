package org.rhq.core.domain.drift;

import javax.transaction.SystemException;

import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDampeningEvent;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.ResourceRepo;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataKey;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationScheduleEntity;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.test.AbstractEJB3Test;

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;

public class DriftChangeSetTest extends AbstractEJB3Test {

    static Class[] entities = new Class[]{
        ResourceRepo.class,
        MeasurementBaseline.class, // baseline BEFORE schedules
        MeasurementDataTrait.class, // traits BEFORE schedules
        CallTimeDataValue.class, // call time data values BEFORE schedules & call time data keys
        CallTimeDataKey.class, // call time data keys BEFORE schedules
        MeasurementOOB.class, //
        MeasurementSchedule.class, // schedules AFTER baselines, traits, and calltime data
        Availability.class,
        ResourceError.class,
        Event.class,
        EventSource.class,
        BundleResourceDeploymentHistory.class,
        // resource deployment history BEFORE resource deployment
        BundleResourceDeployment.class, //
        PackageInstallationStep.class, // steps BEFORE installed package history
        InstalledPackageHistory.class,
        // history BEFORE installed packages & content service requests
        InstalledPackage.class,
        ContentServiceRequest.class,
        ResourceOperationScheduleEntity.class,
        ResourceOperationHistory.class,
        DeleteResourceHistory.class,
        CreateResourceHistory.class,
        ResourceConfigurationUpdate.class,
        // orphan parent list or maps (execute only on non selfRefCascade dbs)
        ResourceConfigurationUpdate.class, // first, delete the raw configs for the config
        ResourceConfigurationUpdate.class, // then delete the config objects
        ResourceConfigurationUpdate.class, // then the history objects wrapping those configs
        PluginConfigurationUpdate.class,
        // orphan parent list or maps (execute only on non selfRefCascade dbs)
        PluginConfigurationUpdate.class, // first, delete the raw configs for the config
        PluginConfigurationUpdate.class, // then delete the config objects
        PluginConfigurationUpdate.class, // then the history objects wrapping those configs
        AlertConditionLog.class, //    Don't
        AlertNotificationLog.class, // alter
        Alert.class, //                order
        AlertCondition.class, //       of
        AlertDampeningEvent.class, //  alert-
        AlertNotification.class, //    related
        AlertDefinition.class, //      deletes
        Drift.class, //       drift before changeset
        DriftChangeSet.class};

    static interface TransactionCallback {
        void execute() throws Exception;
    }

    Resource resource;

    @BeforeGroups(groups = {"drift.changeset"})
    public void initResource() throws Exception {
        resource = new ResourceBuilder().createRandomServer().build();
        resource.setId(0);

        final ResourceType type = resource.getResourceType();
        type.setId(0);

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                getEntityManager().createQuery("delete from DriftChangeSet").executeUpdate();
                getEntityManager().createQuery("delete from Resource").executeUpdate();
                getEntityManager().createQuery("delete from ResourceType").executeUpdate();
            }
        });

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                getEntityManager().persist(type);
                getEntityManager().persist(resource);
            }
        });
    }

    @BeforeMethod(groups = {"drift.changeset"})
    public void setup() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                getEntityManager().createQuery("delete from DriftChangeSet").executeUpdate();
            }
        });
    }

    @Test(groups = {"integration.ejb3", "drift.changeset"})
    public void insertAndLoad() throws Exception {
        final DriftChangeSet changeSet = new DriftChangeSet();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                changeSet.setCategory(COVERAGE);
                changeSet.setVersion(0);
                changeSet.setResource(resource);

                getEntityManager().persist(changeSet);
            }
        });

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                // Verify that we can both load by id and by JPQL to ensure that using a
                // custom type for the id works.

                DriftChangeSet actual = getEntityManager().find(DriftChangeSet.class, changeSet.getId());
                assertNotNull("Failed to load change set by id", actual);

                actual = (DriftChangeSet) getEntityManager().createQuery("from DriftChangeSet where id = :id")
                    .setParameter("id", actual.getId())
                    .getSingleResult();
                assertNotNull("Failed to load change set with JPQL query", actual);
            }
        });
    }

    void executeInTransaction(TransactionCallback callback) {
        try {
            getTransactionManager().begin();
            callback.execute();
            getTransactionManager().commit();
        } catch (Throwable t) {
            try {
                getTransactionManager().rollback();
            } catch (SystemException e) {
                throw new RuntimeException("Failed to rollback transaction", e);
            }
            throw new RuntimeException(t.getCause());
        }
    }

}