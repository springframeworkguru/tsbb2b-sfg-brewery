/*
 *  Copyright 2019 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package guru.springframework.brewery.interceptors;

import guru.springframework.brewery.domain.BeerOrder;
import guru.springframework.brewery.domain.OrderStatusEnum;
import guru.springframework.brewery.events.BeerOrderStatusChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Catch order updates
 */
@Slf4j
@Component
public class OrderHeaderInterceptor extends EmptyInterceptor {

    private final ApplicationEventPublisher publisher;

    public OrderHeaderInterceptor(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {

        if (entity instanceof BeerOrder){
            for(Object curObj : currentState){
                if(curObj instanceof OrderStatusEnum){
                    for (Object prevObj : previousState){
                        if (prevObj instanceof OrderStatusEnum) {
                            OrderStatusEnum curStatus = (OrderStatusEnum) curObj;
                            OrderStatusEnum prevStatus = (OrderStatusEnum) prevObj;

                            if(curStatus != prevStatus){
                                log.debug("Order status change detected");

                                publisher.publishEvent(new BeerOrderStatusChangeEvent((BeerOrder) entity, prevStatus));
                            }
                        }
                    }
                }
            }
        }

        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }
}
