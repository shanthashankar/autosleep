package org.cloudfoundry.autosleep.servicebroker.service;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.autosleep.config.Config;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.scheduling.GlobalWatcher;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AutoSleepServiceInstanceService implements ServiceInstanceService {

    private ServiceRepository repository;

    private GlobalWatcher watcher;


    @Autowired
    public AutoSleepServiceInstanceService(ServiceRepository repository, GlobalWatcher watcher) {
        this.repository = repository;
        this.watcher = watcher;
    }

    @Override
    public org.cloudfoundry.community.servicebroker.model.ServiceInstance createServiceInstance(
            CreateServiceInstanceRequest request) throws
            ServiceInstanceExistsException, ServiceBrokerException {
        log.debug("createServiceInstance - {}", request.getServiceInstanceId());

        AutosleepServiceInstance serviceInstance = repository.findOne(request.getServiceInstanceId());
        if (repository.findOne(request.getServiceInstanceId()) != null) {
            throw new ServiceInstanceExistsException(serviceInstance);
        } else {
            serviceInstance = new AutosleepServiceInstance(request);
            // save in repository before calling remote because otherwise local service binding controler will fail
            // retrieving the service
            repository.save(serviceInstance);
            watcher.watchServiceBindings(request.getServiceInstanceId(), Config.delayBeforeFirstServiceCheck);
        }
        return serviceInstance;
    }

    @Override
    public org.cloudfoundry.community.servicebroker.model.ServiceInstance getServiceInstance(String serviceInstanceId) {
        log.debug("getServiceInstance - {}", serviceInstanceId);
        return repository.findOne(serviceInstanceId);
    }

    @Override
    public org.cloudfoundry.community.servicebroker.model.ServiceInstance updateServiceInstance(
            UpdateServiceInstanceRequest request) throws
            ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        String serviceId = request.getServiceInstanceId();
        log.debug("updateServiceInstance - {}", serviceId);
        AutosleepServiceInstance serviceInstance = repository.findOne(serviceId);
        if (serviceInstance == null) {
            throw new ServiceInstanceDoesNotExistException(serviceId);
        } else {
            serviceInstance = new AutosleepServiceInstance(request);
            repository.save(serviceInstance);
        }
        return serviceInstance;
    }

    @Override
    public org.cloudfoundry.community.servicebroker.model.ServiceInstance deleteServiceInstance(
            DeleteServiceInstanceRequest request) throws ServiceBrokerException {
        log.debug("deleteServiceInstance - {}", request.getServiceInstanceId());
        AutosleepServiceInstance serviceInstance = new AutosleepServiceInstance(request);
        repository.delete(request.getServiceInstanceId());
        return serviceInstance;
    }


}
