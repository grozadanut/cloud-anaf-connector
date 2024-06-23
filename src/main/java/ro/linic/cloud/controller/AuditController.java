package ro.linic.cloud.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/audit")
public class AuditController {

	@Autowired private Javers javers;
	
	@GetMapping("/all")
	public String allChanges(@RequestParam(required = false) final LocalDateTime from, @RequestParam(required = false) final LocalDateTime to,
			@RequestParam(required = false) final String author, @RequestParam(required = false) final String authorFragment,
			@RequestParam(required = false) final Integer limit, @RequestParam(required = false) final Integer skip) {
		final QueryBuilder jqlQuery = QueryBuilder.anyDomainObject()
			.from(from).to(to);
		if (StringUtils.isNotEmpty(author))
			jqlQuery.byAuthor(author);
		if (StringUtils.isNotEmpty(authorFragment))
			jqlQuery.byAuthorLikeIgnoreCase(authorFragment);
		if (limit != null)
			jqlQuery.limit(limit);
		if (skip != null)
			jqlQuery.skip(skip);
		return javers.findChanges(jqlQuery.build()).prettyPrint();
	}

	@GetMapping("/all/snapshots")
	public String allSnapshots(@RequestParam(required = false) final LocalDateTime from, @RequestParam(required = false) final LocalDateTime to,
			@RequestParam(required = false) final String author, @RequestParam(required = false) final String authorFragment,
			@RequestParam(required = false) final Integer limit, @RequestParam(required = false) final Integer skip) {
		final QueryBuilder jqlQuery = QueryBuilder.anyDomainObject()
				.from(from).to(to);
		if (StringUtils.isNotEmpty(author))
			jqlQuery.byAuthor(author);
		if (StringUtils.isNotEmpty(authorFragment))
			jqlQuery.byAuthorLikeIgnoreCase(authorFragment);
		if (limit != null)
			jqlQuery.limit(limit);
		if (skip != null)
			jqlQuery.skip(skip);
		final List<CdoSnapshot> snapshots = javers.findSnapshots(jqlQuery.build());
		return javers.getJsonConverter().toJson(snapshots);
	}

	@GetMapping("/class/{fullyQualifiedName}")
	public String allChangesByClass(@PathVariable("fullyQualifiedName") final String fullyQualifiedName,
			@RequestParam(required = false) final LocalDateTime from, @RequestParam(required = false) final LocalDateTime to,
			@RequestParam(required = false) final String author, @RequestParam(required = false) final String authorFragment,
			@RequestParam(required = false) final Integer limit, @RequestParam(required = false) final Integer skip)
					throws ClassNotFoundException {
		final QueryBuilder jqlQuery = QueryBuilder.byClass(Class.forName(fullyQualifiedName))
				.from(from).to(to);
		if (StringUtils.isNotEmpty(author))
			jqlQuery.byAuthor(author);
		if (StringUtils.isNotEmpty(authorFragment))
			jqlQuery.byAuthorLikeIgnoreCase(authorFragment);
		if (limit != null)
			jqlQuery.limit(limit);
		if (skip != null)
			jqlQuery.skip(skip);
		return javers.findChanges(jqlQuery.build()).prettyPrint();
	}
	
	@GetMapping("/class/{fullyQualifiedName}/shadows")
    public String allShadowsByClass(@PathVariable("fullyQualifiedName") final String fullyQualifiedName,
			@RequestParam(required = false) final LocalDateTime from, @RequestParam(required = false) final LocalDateTime to,
			@RequestParam(required = false) final String author, @RequestParam(required = false) final String authorFragment,
			@RequestParam(required = false) final Integer limit, @RequestParam(required = false) final Integer skip)
					throws ClassNotFoundException {
        final QueryBuilder jqlQuery = QueryBuilder.byClass(Class.forName(fullyQualifiedName)).withChildValueObjects()
        		.from(from).to(to);
        if (StringUtils.isNotEmpty(author))
			jqlQuery.byAuthor(author);
		if (StringUtils.isNotEmpty(authorFragment))
			jqlQuery.byAuthorLikeIgnoreCase(authorFragment);
		if (limit != null)
			jqlQuery.limit(limit);
		if (skip != null)
			jqlQuery.skip(skip);
        final List<Shadow<Object>> shadows = javers.findShadows(jqlQuery.build());
        return javers.getJsonConverter().toJson(shadows);
    }
	
	@GetMapping("/class/{fullyQualifiedName}/snapshots")
	public String allSnapshotsByClass(@PathVariable("fullyQualifiedName") final String fullyQualifiedName,
			@RequestParam(required = false) final LocalDateTime from, @RequestParam(required = false) final LocalDateTime to,
			@RequestParam(required = false) final String author, @RequestParam(required = false) final String authorFragment,
			@RequestParam(required = false) final Integer limit, @RequestParam(required = false) final Integer skip)
					throws ClassNotFoundException {
		final QueryBuilder jqlQuery = QueryBuilder.byClass(Class.forName(fullyQualifiedName))
				.from(from).to(to);
		if (StringUtils.isNotEmpty(author))
			jqlQuery.byAuthor(author);
		if (StringUtils.isNotEmpty(authorFragment))
			jqlQuery.byAuthorLikeIgnoreCase(authorFragment);
		if (limit != null)
			jqlQuery.limit(limit);
		if (skip != null)
			jqlQuery.skip(skip);
		final List<CdoSnapshot> snapshots = javers.findSnapshots(jqlQuery.build());
		return javers.getJsonConverter().toJson(snapshots);
	}
}