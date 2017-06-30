/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.reporting.service.rest;

import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.lang.ServiceException;
import io.mifos.reporting.api.v1.domain.ReportDefinition;
import io.mifos.reporting.api.v1.domain.ReportPage;
import io.mifos.reporting.api.v1.domain.ReportRequest;
import io.mifos.reporting.service.ServiceConstants;
import io.mifos.reporting.service.internal.provider.ReportSpecificationProvider;
import io.mifos.reporting.service.spi.ReportSpecification;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/")
public class ReportingRestController {

  private final Logger logger;
  private final ReportSpecificationProvider reportSpecificationProvider;

  @Autowired
  public ReportingRestController(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                 final ReportSpecificationProvider reportSpecificationProvider) {
    super();
    this.logger = logger;
    this.reportSpecificationProvider = reportSpecificationProvider;
  }

  @Permittable(value = AcceptedTokenType.SYSTEM)
  @RequestMapping(
      value = "/initialize",
      method = RequestMethod.POST,
      consumes = MediaType.ALL_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public
  @ResponseBody
  ResponseEntity<Void> initialize() {
    return ResponseEntity.accepted().build();
  }

  @RequestMapping(
      value = "/categories",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.ALL_VALUE
  )
  public
  ResponseEntity<List<String>> fetchCategories() {
    return ResponseEntity.ok(this.reportSpecificationProvider.getAvailableCategories());
  }

  @RequestMapping(
      value = "categories/{category}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.ALL_VALUE)
  public
  ResponseEntity<List<ReportDefinition>> fetchReportDefinitions(@PathVariable("category") final String category) {
    return ResponseEntity.ok(this.reportSpecificationProvider.getAvailableReports(category));
  }

  @RequestMapping(
      value = "/categories/{category}/reports/{identifier}",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public
  ResponseEntity<ReportPage> generateReport(@PathVariable("category") final String category,
                                           @PathVariable("identifier") final String identifier,
                                           @RequestBody final ReportRequest reportRequest,
                                           @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                           @RequestParam(value = "size", required = false) final Integer size) {

    final Optional<ReportSpecification> optionalReportSpecification =
        this.reportSpecificationProvider.getReportSpecification(category, identifier);
    if (optionalReportSpecification.isPresent()) {
      final ReportSpecification reportSpecification = optionalReportSpecification.get();

      try {
        reportSpecification.validate(reportRequest);
      } catch (final IllegalArgumentException iaex) {
        throw ServiceException.badRequest(iaex.getMessage());
      }

      return ResponseEntity.ok(reportSpecification.generateReport(reportRequest));
    } else {
      throw ServiceException.notFound("Report {0} not found.", identifier);
    }
  }
}
