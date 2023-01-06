package org.noahsark.mqtt.broker.common.spi;

import java.util.List;

/**
 * 文件检索
 *
 * @author zhangxt
 * @date 2023/01/04 16:55
 **/
public class FileSearch implements Search{
    @Override
    public List<String> searchDoc(String keyword) {
        System.out.println("文件搜索 "+keyword);
        return null;
    }
}
