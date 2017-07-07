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
package org.ow2.proactive.catalog.graphql.handler.catalogobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.ow2.proactive.catalog.graphql.bean.common.Operations;
import org.ow2.proactive.catalog.graphql.bean.filter.AndOrArgs;
import org.ow2.proactive.catalog.graphql.bean.filter.CatalogObjectWhereArgs;
import org.ow2.proactive.catalog.graphql.handler.FilterHandler;
import org.ow2.proactive.catalog.repository.entity.CatalogObjectEntity;
import org.ow2.proactive.catalog.repository.specification.catalogobject.AndSpecification;
import org.ow2.proactive.catalog.repository.specification.catalogobject.OrSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;


/**
 * @author ActiveEon Team
 * @since 06/07/2017
 */
@Component
@Log4j2
public class CatalogObjectAndOrGroupFilterHandler
        implements FilterHandler<CatalogObjectWhereArgs, CatalogObjectEntity> {

    @Autowired
    private CatalogObjectBucketIdFilterHandler bucketIdHandler;

    @Autowired
    private CatalogObjectKindFilterHandler kindHandler;

    @Autowired
    private CatalogObjectNameFilterHandler nameHandler;

    @Autowired
    private CatalogObjectMetadataFilterHandler metadataHandler;

    private List<FilterHandler<CatalogObjectWhereArgs, CatalogObjectEntity>> fieldFilterHandlers = new ArrayList<>();

    @PostConstruct
    public void init() {
        fieldFilterHandlers.add(bucketIdHandler);
        fieldFilterHandlers.add(kindHandler);
        fieldFilterHandlers.add(nameHandler);
        fieldFilterHandlers.add(metadataHandler);
    }

    @Override
    public Optional<Specification<CatalogObjectEntity>> handle(CatalogObjectWhereArgs catalogObjectWhereArgs) {
        AndOrArgs andOrArgs;
        Operations operations;

        log.debug(catalogObjectWhereArgs);

        if (catalogObjectWhereArgs.getAndArgs() != null) {
            andOrArgs = catalogObjectWhereArgs.getAndArgs();
            operations = Operations.AND;
        } else {
            andOrArgs = catalogObjectWhereArgs.getOrArgs();
            operations = Operations.OR;
        }

        // binary tree postorder traversal, iterative algo
        if (andOrArgs != null) {
            List<CatalogObjectWhereArgsTreeNode> ret = postOrderTraverseWhereArgsToHaveTreeNodes(andOrArgs, operations);

            Collections.reverse(ret);
            log.debug(ret);

            List<CatalogObjectWhereArgsTreeNode> collect = ret.stream()
                                                              .filter(treeNode -> treeNode.getWhereArgs().size() > 1)
                                                              .collect(Collectors.toList());

            log.debug(collect);

            Specification<CatalogObjectEntity> leftChildSpec = buildFinalSpecification(collect);
            return Optional.of(leftChildSpec);
        }
        return Optional.empty();
    }

    private Specification<CatalogObjectEntity> buildFinalSpecification(List<CatalogObjectWhereArgsTreeNode> collect) {
        Specification<CatalogObjectEntity> leftChildSpec = null;
        Specification<CatalogObjectEntity> rightChildSpec = null;

        // node
        for (CatalogObjectWhereArgsTreeNode argsTreeNode : collect) {

            Operations nodeOperations = argsTreeNode.getOperations();
            boolean leafOnly = true;

            List<Specification<CatalogObjectEntity>> nodeSpecList = new ArrayList<>();

            for (CatalogObjectWhereArgs whereArg : argsTreeNode.getWhereArgs()) {
                if (whereArg.getOrArgs() == null && whereArg.getAndArgs() == null) {
                    for (FilterHandler<CatalogObjectWhereArgs, CatalogObjectEntity> fieldFilterHandler : fieldFilterHandlers) {
                        Optional<Specification<CatalogObjectEntity>> sp = fieldFilterHandler.handle(whereArg);
                        if (sp.isPresent()) {
                            nodeSpecList.add(sp.get());
                            break;
                        }
                    }
                } else {
                    if (leafOnly) {
                        nodeSpecList.add(rightChildSpec);
                    } else {
                        nodeSpecList.add(leftChildSpec);
                    }
                    leafOnly = false;
                }
            }

            Specification<CatalogObjectEntity> temp;
            if (nodeOperations == Operations.AND) {
                temp = AndSpecification.builder().fieldSpcifications(nodeSpecList).build();
            } else {
                temp = OrSpecification.builder().fieldSpcifications(nodeSpecList).build();
            }

            if (leafOnly) {
                rightChildSpec = temp;
            } else {
                leftChildSpec = temp;
            }

        }
        return leftChildSpec;
    }

    private List<CatalogObjectWhereArgsTreeNode> postOrderTraverseWhereArgsToHaveTreeNodes(AndOrArgs andOrArgs,
            Operations operations) {
        Deque<AndOrArgs> stack = new LinkedList<>();
        stack.push(andOrArgs);

        List<CatalogObjectWhereArgsTreeNode> ret = new ArrayList<>();

        while (!stack.isEmpty()) {
            AndOrArgs top = stack.pop();
            if (top != null) {
                ret.add(new CatalogObjectWhereArgsTreeNode(operations, top.getArgs()));

                ArrayList<CatalogObjectWhereArgs> argsCopy = new ArrayList<>(top.getArgs());

                Iterator<CatalogObjectWhereArgs> iterator = argsCopy.iterator();
                AndOrArgs left = null;
                AndOrArgs right = null;
                while (iterator.hasNext()) {
                    CatalogObjectWhereArgs next = iterator.next();
                    if (next.getAndArgs() != null) {
                        operations = Operations.AND;
                        left = next.getAndArgs();
                        iterator.remove();
                        if (!argsCopy.isEmpty()) {
                            right = new AndOrArgs(argsCopy);
                        }
                        break;
                    } else if (next.getOrArgs() != null) {
                        operations = Operations.OR;
                        left = next.getOrArgs();
                        iterator.remove();
                        if (!argsCopy.isEmpty()) {
                            right = new AndOrArgs(argsCopy);
                        }
                        break;
                    }
                }
                stack.push(right);
                stack.push(left);
            }
        }
        return ret;
    }

    @AllArgsConstructor
    @Data
    private static class CatalogObjectWhereArgsTreeNode {

        private Operations operations;

        private List<CatalogObjectWhereArgs> whereArgs;

    }

}
