package org.noahsark.mqtt.broker.common.spi;

import java.util.List;

/**
 * 数据库检索
 *
 * @author zhangxt
 * @date 2023/01/04 16:56
 **/
public class DatabaseSearch implements Search {
    @Override
    public List<String> searchDoc(String keyword) {
        System.out.println("数据搜索 " + keyword);
        return null;
    }
}
