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

package guru.springframework.brewery.services;

import guru.springframework.brewery.domain.BeerInventory;
import guru.springframework.brewery.domain.BeerOrder;
import guru.springframework.brewery.domain.BeerOrderLine;
import guru.springframework.brewery.domain.OrderStatusEnum;
import guru.springframework.brewery.repositories.BeerInventoryRepository;
import guru.springframework.brewery.repositories.BeerOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to allocate inventory to orders.
 *
 */
@Slf4j
@Service
public class BeerOrderAllocationService {

    private final BeerOrderRepository beerOrderRepository;
    private final BeerInventoryRepository beerInventoryRepository;

    public BeerOrderAllocationService(BeerOrderRepository beerOrderRepository, BeerInventoryRepository beerInventoryRepository) {
        this.beerOrderRepository = beerOrderRepository;
        this.beerInventoryRepository = beerInventoryRepository;
    }

    @Transactional
    @Scheduled(fixedRate = 5000) //run every 5 seconds
    public void runBeerOrderAllocation(){
        log.debug("Starting Beer Order Allocation");

        List<BeerOrder> newOrders = beerOrderRepository.findAllByOrderStatus(OrderStatusEnum.NEW);

        if (newOrders.size() > 0 ) {

            log.debug("Number of orders found to allocate: " + newOrders.size());

            newOrders.forEach(beerOrder -> {
                log.debug("Allocating Order" + beerOrder.getCustomerRef());

                AtomicInteger totalOrdered = new AtomicInteger();
                AtomicInteger totalAllocated = new AtomicInteger();

                beerOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                    if ((beerOrderLine.getOrderQuantity() - beerOrderLine.getQuantityAllocated()) > 0) {
                        allocateBeerOrderLine(beerOrderLine);
                    }
                    totalOrdered.set(totalOrdered.get() + beerOrderLine.getOrderQuantity());
                    totalAllocated.set(totalAllocated.get() + beerOrderLine.getQuantityAllocated());
                });

                if(totalOrdered.get() == totalAllocated.get()){
                    log.debug("Order Completely Allocated: " + beerOrder.getCustomerRef());
                    beerOrder.setOrderStatus(OrderStatusEnum.READY);
                }
            });
        } else {
            log.debug("No Orders To Allocate");
        }

        //update orders
        beerOrderRepository.saveAll(newOrders);

    }

    private void allocateBeerOrderLine(BeerOrderLine beerOrderLine) {
        List<BeerInventory> beerInventoryList = beerInventoryRepository.findAllByBeer(beerOrderLine.getBeer());

        beerInventoryList.forEach(beerInventory -> {
            int inventory = (beerInventory.getQuantityOnHand() == null) ? 0 : beerInventory.getQuantityOnHand();
            int orderQty = (beerOrderLine.getOrderQuantity() == null) ? 0 : beerOrderLine.getOrderQuantity() ;
            int allocatedQty = (beerOrderLine.getQuantityAllocated() == null) ? 0 : beerOrderLine.getQuantityAllocated();
            int qtyToAllocate = orderQty - allocatedQty;

            if(inventory >= qtyToAllocate){ // full allocation
                inventory = inventory - qtyToAllocate;
                beerOrderLine.setQuantityAllocated(orderQty);
                beerInventory.setQuantityOnHand(inventory);
            } else if (inventory > 0) { //partial allocation
                beerOrderLine.setQuantityAllocated(allocatedQty + inventory);
                beerInventory.setQuantityOnHand(0);
            }
        });

        beerInventoryRepository.saveAll(beerInventoryList);

        //remove zero records
        List<BeerInventory> zeroRecs = new ArrayList<>();

        beerInventoryList.stream()
                .filter(beerInventory -> beerInventory.getQuantityOnHand() == 0)
                .forEach(zeroRecs::add);

        beerInventoryRepository.deleteAll(zeroRecs);
    }
}
