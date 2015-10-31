package io.trivium.test.cases;

import io.trivium.anystore.AnyServer;
import io.trivium.anystore.ObjectRef;
import io.trivium.anystore.query.Query;
import io.trivium.anystore.query.Value;
import io.trivium.extension._f70b024ca63f4b6b80427238bfff101f.TriviumObject;
import io.trivium.glue.om.Element;
import io.trivium.test.Assert;
import io.trivium.test.TestCase;

import java.util.ArrayList;

public class _f18f295e149f4c2084d80f0056bf0ebf implements TestCase{
    @Override
    public String getTestName() {
        return "load object by type id";
    }

    @Override
    public void run() throws Exception {
        TriviumObject tvm = new TriviumObject();
        Element el = new Element("node","hallo world");

        tvm.setData(el);
        tvm.addMetadata("id", tvm.getId().toString());
        tvm.addMetadata("contentType", "text/plain");
        ObjectRef typeId = ObjectRef.getInstance("39d3af87-5fca-4066-ae7f-b88bc2ae6dc2");
        tvm.setTypeId(typeId);

        AnyServer.INSTANCE.storeObject(tvm);

        Query q = new Query();
        q.criteria.add(new Value("typeId",typeId.toString()));
        ArrayList<TriviumObject> list = AnyServer.INSTANCE.loadObjects(q).getAllAsList();

        Assert.isTrue(typeId==list.get(0).getTypeId());
    }

    @Override
    public ObjectRef getTypeId() {
        return ObjectRef.getInstance("f18f295e-149f-4c20-84d8-0f0056bf0ebf");
    }
}

