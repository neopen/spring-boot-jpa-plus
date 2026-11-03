package io.github.neopen.jpa.bean.param;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageParamTest {

    @Test
    void acceptsSmallPageSizesAndCapsLargeRequests() {
        PageParam param = new PageParam();

        param.setLimit(2);
        assertEquals(2, param.getLimit());

        param.setLimit(10_000);
        assertEquals(PageParam.MAX_PAGE_SIZE, param.getLimit());
    }

    @Test
    void calculatesOneBasedPageOffsetWithoutIntegerOverflow() {
        PageParam param = new PageParam();
        param.setStart(3);
        param.setLimit(20);

        assertEquals(40, param.myStart());
    }
}
