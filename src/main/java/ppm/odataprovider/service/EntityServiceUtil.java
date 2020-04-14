package ppm.odataprovider.service;

import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;

public class EntityServiceUtil {

    public static EdmEntitySet getNavigationTargetEntitySet(EdmEntitySet sourceEntitySet, EdmNavigationProperty navProp) {
        EdmEntitySet targetEntitySet = null;
        EdmBindingTarget bindingTarget = sourceEntitySet.getRelatedBindingTarget(navProp.getName());
        if (bindingTarget instanceof EdmEntitySet) {
            targetEntitySet = (EdmEntitySet) bindingTarget;
        }
        return targetEntitySet;
    }
}
