/*
 * MIT License
 * Copyright (c) 2020 Curt Binder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package info.curtbinder.java.MigrateAAtoAN;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ParamTests {

    private static final BigDecimal BD_INVALID = new BigDecimal("-1");
    private static final Integer I_INVALID = Integer.MAX_VALUE;

    LocalDateTime dateTime;
    BigDecimal ph;
    BigDecimal temperature;
    BigDecimal salinity;
    BigDecimal alkalinity;
    Integer calcium;
    Integer magnesium;
    BigDecimal nitrate;
    Integer phosphorus;
    String notes;

    ParamTests() {
        dateTime = LocalDateTime.now();
        ph = BD_INVALID;
        temperature = BD_INVALID;
        salinity = BD_INVALID;
        alkalinity = BD_INVALID;
        calcium = I_INVALID;
        magnesium = I_INVALID;
        nitrate = BD_INVALID;
        phosphorus = I_INVALID;
        notes = "";
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public BigDecimal getPh() {
        return ph;
    }

    public void setPh(BigDecimal ph) {
        this.ph = ph;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getSalinity() {
        return salinity;
    }

    public void setSalinity(BigDecimal salinity) {
        this.salinity = salinity;
    }

    public BigDecimal getAlkalinity() {
        return alkalinity;
    }

    public void setAlkalinity(BigDecimal alkalinity) {
        this.alkalinity = alkalinity;
    }

    public Integer getCalcium() {
        return calcium;
    }

    public void setCalcium(Integer calcium) {
        this.calcium = calcium;
    }

    public Integer getMagnesium() {
        return magnesium;
    }

    public void setMagnesium(Integer magnesium) {
        this.magnesium = magnesium;
    }

    public BigDecimal getNitrate() {
        return nitrate;
    }

    public void setNitrate(BigDecimal nitrate) {
        this.nitrate = nitrate;
    }

    public Integer getPhosphorus() {
        return phosphorus;
    }

    public void setPhosphorus(Integer phosphorus) {
        this.phosphorus = phosphorus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPhString() {
        if (ph.compareTo(BD_INVALID) == 0) {
            return "";
        }
        return ph.toString();
    }

    public String getTemperatureString() {
        if (temperature.compareTo(BD_INVALID) == 0) {
            return "";
        }
        return temperature.toString();
    }

    public String getSalinityString() {
        if (salinity.compareTo(BD_INVALID) == 0) {
            return "";
        }
        return salinity.toString();
    }

    public String getAlkalinityString() {
        if (alkalinity.compareTo(BD_INVALID) == 0) {
            return "";
        }
        return alkalinity.toString();
    }

    public String getCalciumString() {
        if (calcium.compareTo(I_INVALID) == 0) {
            return "";
        }
        return calcium.toString();
    }

    public String getMagnesiumString() {
        if (magnesium.compareTo(I_INVALID) == 0) {
            return "";
        }
        return magnesium.toString();
    }

    public String getNitrateString() {
        if (nitrate.compareTo(BD_INVALID) == 0) {
            return "";
        }
        return nitrate.toString();
    }

    public String getPhosphorusString() {
        if (phosphorus.compareTo(I_INVALID) == 0) {
            return "";
        }
        return phosphorus.toString();
    }

    public String titleString() {
        return "Water Test: " + dateTime.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    @Override
    public String toString() {
        return "" + dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) +
                " ph=" + ph +
                ", temperature=" + temperature +
                ", salinity=" + salinity +
                ", alkalinity=" + alkalinity +
                ", calcium=" + calcium +
                ", magnesium=" + magnesium +
                ", nitrate=" + nitrate +
                ", phosphorus=" + phosphorus +
                ", notes='" + notes + '\'';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ParamTests) {
            ParamTests pt = (ParamTests) obj;
            return (this.dateTime.getYear() == pt.dateTime.getYear()) &&
                    (this.dateTime.getMonthValue() == pt.dateTime.getMonthValue()) &&
                    (this.dateTime.getDayOfMonth() == pt.dateTime.getDayOfMonth());
        } else {
            return false;
        }
    }
}
