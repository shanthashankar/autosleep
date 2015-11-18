package org.cloudfoundry.autosleep.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.autosleep.dao.model.ApplicationBinding;
import org.cloudfoundry.autosleep.dao.model.ApplicationInfo;
import org.cloudfoundry.autosleep.dao.model.AutosleepServiceInstance;
import org.cloudfoundry.autosleep.dao.repositories.ApplicationRepository;
import org.cloudfoundry.autosleep.dao.repositories.BindingRepository;
import org.cloudfoundry.autosleep.dao.repositories.ServiceRepository;
import org.cloudfoundry.autosleep.remote.ApplicationActivity;
import org.cloudfoundry.autosleep.remote.ApplicationIdentity;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DebugControllerTest {

    private static final UUID applicationId = UUID.randomUUID();

    private static final String serviceInstanceId = "serviceInstanceId";

    private static final String serviceBindingId = "serviceBindingId";

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BindingRepository bindingRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private Catalog catalog;

    @InjectMocks
    private DebugController debugController;


    private MockMvc mockMvc;

    private ObjectMapper objectMapper;


    @Before
    public void init() {
        objectMapper = new ObjectMapper();
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(debugController).build();


        when(catalog.getServiceDefinitions()).thenReturn(Collections.singletonList(
                new ServiceDefinition("serviceDefinitionId", "serviceDefinition", "", true,
                        Collections.singletonList(new Plan("planId", "plan", "")))));


    }

    @Test
    public void testInstances() throws Exception {
        mockMvc.perform(get("/admin/debug/")
                .accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Test
    public void testBindings() throws Exception {
        mockMvc.perform(get("/admin/debug/" + serviceInstanceId + "/bindings/")
                .accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Test
    public void testApplications() throws Exception {
        mockMvc.perform(get("/admin/debug/applications/")
                .accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }


    @Test
    public void testListInstances() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AutosleepServiceInstance.INACTIVITY_PARAMETER, "PT15M");
        parameters.put(AutosleepServiceInstance.EXCLUDE_PARAMETER, ".*");
        CreateServiceInstanceRequest createRequestTemplate = new CreateServiceInstanceRequest("definition",
                "plan",
                "org",
                "space", parameters);
        AutosleepServiceInstance serviceInstance = new AutosleepServiceInstance(
                createRequestTemplate.withServiceInstanceId(serviceInstanceId));
        when(serviceRepository.findAll()).thenReturn(Collections.singletonList(serviceInstance));


        mockMvc.perform(get("/admin/debug/services/instances/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content()
                .contentType(new MediaType(MediaType.APPLICATION_JSON,
                        Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))))
                .andDo(mvcResult -> {
                    verify(serviceRepository, times(1)).findAll();
                    AutosleepServiceInstance[] serviceInstances = objectMapper
                            .readValue(mvcResult.getResponse().getContentAsString(), AutosleepServiceInstance[].class);
                    assertThat(serviceInstances.length, is(equalTo(1)));
                    assertThat(serviceInstances[0].getServiceInstanceId(), is(equalTo(serviceInstanceId)));
                });
    }

    @Test
    public void testListBindings() throws Exception {
        ApplicationBinding serviceBinding = new ApplicationBinding(serviceBindingId, serviceInstanceId,
                null, null, UUID.randomUUID().toString());
        when(bindingRepository.findAll()).thenReturn(Collections.singletonList(serviceBinding));

        mockMvc.perform(get("/admin/debug/services/bindings/" + serviceInstanceId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content()
                .contentType(new MediaType(MediaType.APPLICATION_JSON,
                        Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))))
                .andDo(mvcResult -> {
                    verify(bindingRepository, times(1)).findAll();
                    ApplicationBinding[] serviceBindings = objectMapper
                            .readValue(mvcResult.getResponse().getContentAsString(), ApplicationBinding[].class);
                    assertThat(serviceBindings.length, is(equalTo(1)));
                    assertThat(serviceBindings[0].getId(), is(equalTo(serviceBindingId)));


                });
        mockMvc.perform(get("/admin/debug/services/bindings/" + serviceInstanceId + "-tmp")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content()
                .contentType(new MediaType(MediaType.APPLICATION_JSON,
                        Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))))
                .andDo(mvcResult -> {
                    verify(bindingRepository, times(2)).findAll();
                    ApplicationBinding[] serviceBindings = objectMapper
                            .readValue(mvcResult.getResponse().getContentAsString(), ApplicationBinding[].class);
                    assertThat(serviceBindings.length, is(equalTo(0)));
                });
    }

    @Test
    public void testListApplications() throws Exception {
        ApplicationInfo applicationInfo = new ApplicationInfo(applicationId).withRemoteInfo(new ApplicationActivity(
                new ApplicationIdentity(applicationId, "applicationName"),
                AppState.STARTED, Instant.now(), Instant.now()));
        when(applicationRepository.findAll()).thenReturn(Collections.singletonList(applicationInfo));

        mockMvc.perform(get("/admin/debug/services/applications/")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType(new MediaType(MediaType.APPLICATION_JSON,
                                Collections.singletonMap("charset", Charset.forName("UTF-8").toString()))))
                .andDo(mvcResult -> {
                    verify(applicationRepository, times(1)).findAll();
                    ApplicationInfo[] applicationInfos = objectMapper
                            .readValue(mvcResult.getResponse().getContentAsString(), ApplicationInfo[].class);
                    assertThat(applicationInfos.length, is(equalTo(1)));
                    assertThat(applicationInfos[0].getUuid(), is(equalTo(applicationId)));
                });
    }

    @Test
    public void testDeleteApplication() throws Exception {
        String applicationId = "applicationToDelete";
        mockMvc.perform(delete("/admin/debug/services/applications/" + applicationId))
                .andExpect(status().is(HttpStatus.NO_CONTENT.value()))
                .andDo(mvcResult -> verify(applicationRepository, times(1)).delete(eq(applicationId)));


    }


}