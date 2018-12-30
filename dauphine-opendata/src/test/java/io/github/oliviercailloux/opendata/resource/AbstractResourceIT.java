package io.github.oliviercailloux.opendata.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;

import com.google.common.base.Preconditions;

import io.github.oliviercailloux.opendata.TestUtils;
import io.github.oliviercailloux.opendata.dao.Dao;
import io.github.oliviercailloux.opendata.dao.DaoException;

public abstract class AbstractResourceIT<E extends io.github.oliviercailloux.opendata.entity.Entity, D extends Dao<E>> {

	protected static final String WAR_NAME = "resource-it-war";
	protected static final String BASE_URL = "http://localhost:8888/" + WAR_NAME + "/resource/";

	protected final Client client;
	protected final String resourcePath;
	protected D dao;

	public AbstractResourceIT(final Client client, final String resourcePath) {
		this.client = Preconditions.checkNotNull(client);
		this.resourcePath = Preconditions.checkNotNull(resourcePath);
	}

	public AbstractResourceIT(final String resourcePath) {
		this(ResteasyClientBuilder.newClient(), resourcePath);
	}

	public void setDao(final D dao) {
		this.dao = Preconditions.checkNotNull(dao);
	}

	@After
	public void closeClient() {
		client.close();
	}

	public static WebArchive makeWar() {
		return TestUtils.makeWar(WAR_NAME);
	}

	protected abstract GenericType<List<E>> getEntitiesType();

	protected WebTarget getWebTarget() {
		return client.target(BASE_URL);
	}

	protected WebTarget getResourceWebTarget() {
		return getWebTarget().path(resourcePath);
	}

	protected WebTarget getResourceWebTarget(final String path) {
		return getResourceWebTarget().path(path);
	}

	protected WebTarget getWebTarget(final String path) {
		return getWebTarget().path(path);
	}

	protected Builder acceptCharset(final String charset, final Builder builder) {
		builder.header(HttpHeaders.ACCEPT_CHARSET, charset);
		return builder;
	}

	protected Builder acceptUTF8(final Builder builder) {
		return acceptCharset(StandardCharsets.UTF_8.name(), builder);
	}

	protected Builder acceptLanguage(final String language, final Builder builder) {
		builder.header(HttpHeaders.ACCEPT_LANGUAGE, language);
		return builder;
	}

	protected Builder acceptEnglish(final Builder builder) {
		return acceptLanguage(Locale.ENGLISH.getLanguage(), builder);
	}

	protected Builder accept(final String accept, final Builder builder) {
		builder.header(HttpHeaders.ACCEPT, accept);
		return builder;
	}

	protected Builder acceptJson(final Builder builder) {
		return accept(MediaType.APPLICATION_JSON, builder);
	}

	protected Builder acceptXml(final Builder builder) {
		return accept(MediaType.APPLICATION_XML, builder);
	}

	protected Builder acceptJsonUTF8English(final String path) {
		final Builder builder = getResourceWebTarget(path).request();
		acceptEnglish(builder);
		acceptJson(builder);
		acceptUTF8(builder);
		return builder;
	}

	protected Builder acceptJsonUTF8English() {
		final Builder builder = getResourceWebTarget().request();
		acceptEnglish(builder);
		acceptJson(builder);
		acceptUTF8(builder);
		return builder;
	}

	protected InputStream getInputStream(final Response response) {
		return (InputStream) response.getEntity();
	}

	protected Scanner getScanner(final Response response) throws IOException {
		return new Scanner(new BufferedInputStream(getInputStream(response)));
	}

	protected <T extends io.github.oliviercailloux.opendata.entity.Entity> T getEntity(final Response response,
			final Class<T> entityType) {
		response.bufferEntity();
		return response.readEntity(entityType);
	}

	protected void assertStatusCodeIs(final int expectedStatusCode, final Response response) {
		assertEquals("status code KO", expectedStatusCode, response.getStatus());
	}

	protected void assertStatusIsOk(final Response response) {
		assertStatusCodeIs(HttpServletResponse.SC_OK, response);
	}

	protected void assertStatusIsNoContent(final Response response) {
		assertStatusCodeIs(HttpServletResponse.SC_NO_CONTENT, response);
	}

	protected void assertStatusIsNotFound(final Response response) {
		assertStatusCodeIs(HttpServletResponse.SC_NOT_FOUND, response);
	}

	protected void assertStatusIsBadRequest(final Response response) {
		assertStatusCodeIs(HttpServletResponse.SC_BAD_REQUEST, response);
	}

	protected void assertStatusIsCreated(final Response response) {
		assertStatusCodeIs(HttpServletResponse.SC_CREATED, response);
	}

	protected void assertContentTypeIs(final String contentType, final Response response) {
		assertEquals("Content-Type KO", contentType, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
	}

	protected void assertContentTypeIsJsonUTF8(final Response response) {
		assertContentTypeIs("application/json; charset=UTF-8", response);
	}

	protected void assertContentTypeIsXmlUTF8(final Response response) {
		assertContentTypeIs("application/xml; charset=UTF-8", response);
	}

	protected void assertBodyIs(final String expectedBody, final Response response) throws IOException {
		assertEquals("Body KO", expectedBody, getScanner(response).nextLine());
	}

	protected <T extends io.github.oliviercailloux.opendata.entity.Entity> void assertEntityIs(final T expectedEntity,
			final Response response) {
		assertEquals("Entity KO", expectedEntity, getEntity(response, expectedEntity.getClass()));
	}

	protected abstract E makeEntity();

	@Test
	public void testGet() throws IOException, DaoException {
		dao.persist(makeEntity());
		dao.flush();
		final List<E> entities = dao.findAll();
		final Response response = acceptJsonUTF8English().get();
		assertStatusIsOk(response);
		assertContentTypeIsJsonUTF8(response);

		response.bufferEntity();
		final GenericType<List<E>> entitiesType = getEntitiesType();
		final List<E> receivedEntities = response.readEntity(entitiesType);
		assertEquals(receivedEntities, entities);
	}

	@Test
	public void testGetId() throws IOException, DaoException {
		final E c = makeEntity();
		final E persistedEntity = dao.persist(c);
		final Response response = acceptJsonUTF8English(persistedEntity.getId().toString()).get();
		assertStatusIsOk(response);
		assertContentTypeIsJsonUTF8(response);
		assertEntityIs(persistedEntity, response);
	}

	@Test
	public void testGetIdBadRequest() throws IOException, DaoException {
		final Response response = acceptJsonUTF8English("abc").get();
		assertStatusIsBadRequest(response);
	}

	@Test
	public void testGetIdNotFound() throws IOException, DaoException {
		final Response response = acceptJsonUTF8English("123456789").get();
		assertStatusIsNotFound(response);
	}

	@Test
	public void testPostAlreadyExists() throws DaoException {
		final E c = makeEntity();
		final E persistedEntity = dao.persist(c);
		final Response response = acceptJsonUTF8English().post(Entity.json(persistedEntity));
		assertStatusCodeIs(Status.CONFLICT.getStatusCode(), response);
	}

	@Test
	public void testPutPersist() {
		final E c = makeEntity();
		final Response response = acceptJsonUTF8English().put(Entity.json(c));
		assertStatusIsCreated(response);
	}

	@Test
	public void testPutMerge() throws DaoException {
		final E c = makeEntity();
		final E persistedEntity = dao.persist(c);
		final Response response = acceptJsonUTF8English().put(Entity.json(persistedEntity));
		assertStatusIsNoContent(response);
	}

	@Test
	public void testDelete() throws DaoException {
		final E c = makeEntity();
		final E persistedEntity = dao.persist(c);
		final Response response = acceptJsonUTF8English(persistedEntity.getId().toString()).delete();
		assertStatusIsNoContent(response);
		assertNull("entity was not removed", dao.findOne(persistedEntity.getId()));
	}

	@Test
	public void testDeleteNotExisting() throws DaoException {
		final Response response = acceptJsonUTF8English("-1").delete();
		assertStatusIsNotFound(response);
	}

}