package org.noahsark.mqtt.broker.common;

import org.junit.Test;
import org.noahsark.mqtt.broker.common.spi.Search;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * SPI test
 *
 * @author zhangxt
 * @date 2023/01/04 16:52
 **/
public class SpiTest {

    @Test
    public void searchTest() {

        ServiceLoader<Search> s = ServiceLoader.load(Search.class);
        Iterator<Search> iterator = s.iterator();

        while (iterator.hasNext()) {
            Search search =  iterator.next();
            search.searchDoc("hello world");
        }
    }

}
