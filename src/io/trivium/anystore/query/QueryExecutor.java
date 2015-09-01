/*
 * Copyright 2015 Jens Walter
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

package io.trivium.anystore.query;

import io.trivium.anystore.AnyIndex;
import io.trivium.anystore.AnyServer;
import io.trivium.anystore.ObjectRef;
import io.trivium.glue.TriviumObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class QueryExecutor implements Supplier<TriviumObject> {
    Logger log = Logger.getLogger(getClass().getName());
    
    ObjectRef id;
    Query query;
    ArrayList<ObjectRef> keys = new ArrayList<>();
    Iterator<ObjectRef> iterator;

    public QueryExecutor(Query query) {
        id = query.id;
        this.query = query;
    }

    /**
     * runs the query an return, whether there is a resultset
     */
    public boolean execute() {
        boolean mayExist = true;
        ArrayList<String> key = new ArrayList<>();
        for (Criteria crit : query.criteria) {
            if (crit instanceof Value) {
                Value val = (Value) crit;
                //check for result probability
                boolean returnCode = AnyIndex.check(val.getName(), val.getValue());
                if (returnCode == false)
                    mayExist = false;
                else {
                    key.add(val.getName());
                }
            }
        }
        if (!mayExist) {
            return false;
        } else {
            //sequential index scan
            String indexName = "typeId";
            for (int i = 0; i < key.size(); i++) {
                if (AnyIndex.getVariance(key.get(i)) > AnyIndex.getVariance(indexName)) {
                    indexName = key.get(i);
                }
            }
            String value = query.getValueForName(indexName);
            Stream<ObjectRef> stream = AnyIndex.lookup(indexName, value);
            stream.forEach(e -> keys.add(e));
            iterator = keys.iterator();
            return true;
        }
    }

    public int getSize() {
        return keys.size();
    }

    public TriviumObject get() {
        if (iterator.hasNext()) {
            ObjectRef ref = iterator.next();
            try {
                TriviumObject po = AnyServer.INSTANCE.getStore().loadObject(ref);
                //check for correct value
                boolean valid = true;
                for (Criteria crit : query.criteria) {
                    if (crit instanceof Value) {
                        Value val = (Value) crit;
                        if(! (po.hasMetaKey(val.getName()) &&
                                po.findMetaValue(val.getName()).equals(val.getValue()))){
                            valid=false;
                        }
                    }else
                    if(crit instanceof Range) {
                        Range range = (Range) crit;
                        if(range.getRangeOption() == RangeType.within){
                            if( po.hasMetaKey(range.getName())){
                                String value = po.findMetaValue(range.getName());
                                //check for int type
                                try {
                                    double d_value = Double.parseDouble(value);
                                }catch(NumberFormatException nfe){
                                    log.log(Level.FINE,"looking for number, but value is not convertible", nfe);
                                }
                            }
                        }
                    }
                }
                if(valid) {
                    return po;
                } else {
                    return get();
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE,"ignoring error while loading object", ex);
            }
        }
        return null;
    }
}
