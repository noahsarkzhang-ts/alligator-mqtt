package org.noahsark.mqtt.broker.common.spi;

import java.util.List;

/**
 * 文件检索
 *
 * @author zhangxt
 * @date 2023/01/04 16:53
 **/
public interface Search {
    List<String> searchDoc(String keyword);
}
