/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.services.query.graphql.web;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.activiti.services.query.qraphql.web.ActivitiGraphQLController;
import org.activiti.services.query.qraphql.web.ActivitiGraphQLController.GraphQLQueryRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaExecutor;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ActivitiGraphQLController.class)
public class ActivitiGraphQLControllerTest {
    
	@Autowired 
	private MockMvc			mockmvc;
	
	@MockBean
	private GraphQLJpaExecutor	executor;
	
	@Autowired
	private ObjectMapper	mapper;

    @Configuration
    @Import(ActivitiGraphQLController.class)
    static class Config { }
	
	private void ok(final GraphQLQueryRequest query) throws Exception, JsonProcessingException {
		perform(mapper.writeValueAsString(query)).andExpect(status().isOk());
	}

	private void ok(final String json) throws Exception, JsonProcessingException {
		perform(json).andExpect(status().isOk());
	}

	private ResultActions perform(final String json) throws Exception {
		return mockmvc.perform(post("/graphql").content(json).contentType(MediaType.APPLICATION_JSON));
	}

	// Serialize a Query object
	@Test
	public void testGraphqlQuery() throws Exception {
		ok(new GraphQLQueryRequest("{Tasks(where: {name: {EQ: \"name\"}}){select{id}}}"));
		verify(executor).execute("{Tasks(where: {name: {EQ: \"name\"}}){select{id}}}", null);
	}

	@Test
	public void testGraphqlQueryNull() throws Exception {
		perform(mapper.writeValueAsString(new GraphQLQueryRequest(null))).andExpect(status().isBadRequest());
	}

	@SuppressWarnings( "serial" )
    @Test
	public void testGraphqlArguments() throws Exception {
		GraphQLQueryRequest query = new GraphQLQueryRequest("query TasksQuery($title: String!){Tasks(where:{name: {EQ: $title}}){select{id name}}}");
		query.setVariables("{\"title\":\"value\"}");
		
		Map<String, Object> targetVariables = new HashMap<>();
		targetVariables.put("title", "value"); 
		
		ok(query);
		verify(executor).execute(
		    query.getQuery(),
		    targetVariables
		 );
	}

	// Json directly
	@Test
	public void testGraphqlArgumentsJson() throws Exception {
		String json = "{\"query\": \"{Tasks(where:{name:{EQ: \\\"title\\\"}}){select{ title genre }}\", \"arguments\": {\"title\": \"title\"}}";
		ok(json);
		verify(executor).execute("{Tasks(where:{name:{EQ: \"title\"}}){select{ title genre }}", null);
	}

	@Test
	public void testGraphqlArgumentsEmptyString() throws Exception {
		String json = "{\"query\": \"{Tasks(where:{name:{EQ: \\\"title\\\"}}){select{id name}}\", \"arguments\": \"\"}";
		ok(json);
		verify(executor).execute("{Tasks(where:{name:{EQ: \"title\"}}){select{id name}}", null);
	}

	@Test
	public void testGraphqlArgumentsNull() throws Exception {
		String json = "{\"query\": \"{Tasks(where:{name:{EQ: \\\"title\\\"}}){select{id name}}\", \"arguments\": null}";
		ok(json);
		verify(executor).execute("{Tasks(where:{name:{EQ: \"title\"}}){select{id name}}", null);
	}

	// Form submitted data
	@Test
	public void testGraphqlArgumentsParams() throws Exception {
		String query = "{Tasks(title: \"title\"){title genre}}";
		mockmvc.perform(post("/graphql").param("query", query).contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk());
		verify(executor).execute(query, null);
	}

	@Test
	public void testGraphqlArgumentsParamsVariables() throws Exception {
		String query = "query TasksQuery($title: String!){Tasks(name: $title){id name}}";
		Map<String, Object> args = new HashMap<>();
		args.put("title", "value");
		String argsStr = mapper.writeValueAsString(args);
		mockmvc.perform(post("/graphql").param("query", query).param("variables", argsStr).contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk());
		verify(executor).execute(query, args);
	}

	@Test
	public void testGraphqlArgumentsParamsVariablesEmpty() throws Exception {
		String query = "{Tasks(name: \"title\"){id name}}";
		mockmvc.perform(post("/graphql").param("query", query).param("variables", "").contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk());
		verify(executor).execute(query, null);
	}
}
