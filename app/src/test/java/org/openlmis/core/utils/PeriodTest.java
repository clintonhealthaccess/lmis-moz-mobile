package org.openlmis.core.utils;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PeriodTest {
    @Test
    public void shouldDetermineItsOwnBeginAndEnd() throws Exception {
        testPeriodBeginEnd("2015-02-03", "2015-01-21", "2015-02-20");//normal date
        testPeriodBeginEnd("2016-08-20", "2016-07-21", "2016-08-20");//on end
        testPeriodBeginEnd("2014-06-21", "2014-06-21", "2014-07-20");//on begin
        testPeriodBeginEnd("2014-12-25", "2014-12-21", "2015-01-20");//cross year
    }

    private void testPeriodBeginEnd(String anyDayInPeriod, String begin, String end) {
        //given
        DateTime anyDay = DateTime.parse(anyDayInPeriod);

        //when
        Period period = new Period(anyDay);

        //then
        assertThat(period.getBegin(), is(DateTime.parse(begin)));
        assertThat(period.getEnd(), is(DateTime.parse(end)));
    }
}