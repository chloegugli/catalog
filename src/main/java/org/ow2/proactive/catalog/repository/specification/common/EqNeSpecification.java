/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.catalog.repository.specification.common;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.ow2.proactive.catalog.graphql.bean.common.Operations;
import org.ow2.proactive.catalog.repository.entity.CatalogObjectEntity;
import org.ow2.proactive.catalog.repository.entity.metamodel.CatalogObjectEntityMetaModelEnum;
import org.springframework.data.jpa.domain.Specification;

import lombok.AllArgsConstructor;
import lombok.Builder;


/**
 * @author ActiveEon Team
 * @since 05/07/2017
 */
@AllArgsConstructor
@Builder
public class EqNeSpecification<T> implements Specification<CatalogObjectEntity> {

    protected CatalogObjectEntityMetaModelEnum entityMetaModelEnum;

    protected Operations operations;

    protected T value;

    @Override
    public Predicate toPredicate(Root<CatalogObjectEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        switch (operations) {
            case EQ:
                return cb.equal(root.<T> get(entityMetaModelEnum.getName()), value);
            case NE:
                return cb.notEqual(root.<T> get(entityMetaModelEnum.getName()), value);
            default:
                throw new IllegalStateException(operations + " is not supported");
        }
    }
}
