/*
 * Copyright 2016 Jens Walter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.trivium.extension.fact;

import io.trivium.anystore.TypeRef;
import io.trivium.extension.Typed;
import io.trivium.glue.om.Element;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public interface Fact extends Typed {
    default String getFactName(){
        String name = this.getClass().getCanonicalName();
        return name.substring(name.lastIndexOf('.')+1)+" ["+name+"]";
    }

    default void populate(TriviumObject tvm){
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            Element el = tvm.getData();
            el = el.getChild(0);
            for (Field field : fields) {
                String name = field.getName();
                field.setAccessible(true);
                try {
                    if (field.getType() == long.class) {
                        field.setLong(this, Long.parseLong(el.getFirstChild(name).getValue()));
                    } else if (field.getType() == int.class) {
                        field.setInt(this, Integer.parseInt(el.getFirstChild(name).getValue()));
                    } else if (field.getType() == boolean.class) {
                        field.setBoolean(this, Boolean.parseBoolean(el.getFirstChild(name).getValue()));
                    } else if (field.getType() == byte.class) {
                        field.setByte(this, Byte.parseByte(el.getFirstChild(name).getValue()));
                    } else if (field.getType() == float.class) {
                        field.setFloat(this, Float.parseFloat(el.getFirstChild(name).getValue()));
                    } else if (field.getType() == double.class) {
                        field.setDouble(this, Double.parseDouble(el.getFirstChild(name).getValue()));
                    } else if (field.getType() == short.class) {
                        field.setShort(this, Short.parseShort(el.getFirstChild(name).getValue()));
                    } else if (field.getType() == char.class) {
                        field.setChar(this, el.getFirstChild(name).getValue().charAt(0));
                    } else if (field.getType() == AtomicLong.class) {
                        field.set(this, new AtomicLong(Long.parseLong(el.getFirstChild(name).getValue())));
                    } else {
                        //try String
                        field.set(this, el.getFirstChild(name).getValue());
                    }
                } catch (Exception ex) {
                    getLogger().log(Level.SEVERE,"error while building typed object", ex);
                }
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE,"error while constructing typed object", ex);
        }
    }

    default TriviumObject toTriviumObject(){
        Class<?> aClass = this.getClass();
        Field[] fields = aClass.getDeclaredFields();
        TriviumObject tvm = new TriviumObject();

        tvm.setTypeRef(TypeRef.getInstance(aClass.getCanonicalName()));
        Element root = new Element("dummy");
        for(Field field : fields){
            String name = field.getName();
            field.setAccessible(true);
            try {
                if (field.getType() == long.class || field.getType() == int.class
                        || field.getType() == boolean.class || field.getType() == byte.class || field.getType() == float.class
                        || field.getType() == double.class || field.getType() == short.class || field.getType() == char.class
                        || field.getType() == String.class) {
                    Object obj = field.get(this);
                    if(obj != null) {
                        root.addChild(new Element(name, obj.toString()));
                    }
                }
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE,"error while building typed object", (Throwable)ex);
            }
        }
        tvm.setData(root);
        return tvm;
    }
}
