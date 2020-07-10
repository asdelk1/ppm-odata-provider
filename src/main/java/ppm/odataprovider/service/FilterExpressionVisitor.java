package ppm.odataprovider.service;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.*;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import java.util.List;
import java.util.Locale;

public class FilterExpressionVisitor implements ExpressionVisitor<Object> {

    @Override
    public Object visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException {
        String literalAsString = literal.getText();
        if (literal.getType() instanceof EdmString) {
            String stringLiteral = "";
            if (literal.getText().length() > 2) {
                stringLiteral = literalAsString.substring(1, literalAsString.length() - 1);
            }

            return stringLiteral;
        } else {
            // Try to convert the literal into an Java Integer
            try {
                return Integer.parseInt(literalAsString);
            } catch (NumberFormatException e) {
                throw new ODataApplicationException("Only Edm.Int32 and Edm.String literals are implemented",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
            }
        }
    }

    @Override
    public Object visitMember(Member member) throws ExpressionVisitException, ODataApplicationException {
        List<UriResource> resourceParts = member.getResourcePath().getUriResourceParts();
        if (resourceParts.size() == 1 && resourceParts.get(0) instanceof UriResourcePrimitiveProperty) {
            return ((UriResourcePrimitiveProperty) resourceParts.get(0)).getProperty();
        }

        throw new ODataApplicationException(member.getResourcePath() + " is not a valid property of an entity.",
                HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind binaryOperatorKind, Object o, Object t1) throws ExpressionVisitException, ODataApplicationException {
        if (binaryOperatorKind == BinaryOperatorKind.ADD
                || binaryOperatorKind == BinaryOperatorKind.MOD
                || binaryOperatorKind == BinaryOperatorKind.MUL
                || binaryOperatorKind == BinaryOperatorKind.DIV
                || binaryOperatorKind == BinaryOperatorKind.SUB) {
            return evaluateArithmeticOperation(binaryOperatorKind, o, t1);
        } else if (binaryOperatorKind == BinaryOperatorKind.EQ
                || binaryOperatorKind == BinaryOperatorKind.NE
                || binaryOperatorKind == BinaryOperatorKind.GE
                || binaryOperatorKind == BinaryOperatorKind.GT
                || binaryOperatorKind == BinaryOperatorKind.LE
                || binaryOperatorKind == BinaryOperatorKind.LT) {
            return evaluateComparisonOperation(binaryOperatorKind, o, t1);
        } else if (binaryOperatorKind == BinaryOperatorKind.AND
                || binaryOperatorKind == BinaryOperatorKind.OR) {
            return evaluateBooleanOperation(binaryOperatorKind, o, t1);
        } else {
            throw new ODataApplicationException("Binary operation " + binaryOperatorKind.name() + " is not implemented",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
    }

    private Object evaluateBooleanOperation(BinaryOperatorKind operator, Object left, Object right)
            throws ODataApplicationException {

        // First check that both operands are of type Boolean
        if (left instanceof Criterion && right instanceof Criterion) {
            Criterion valueLeft = (Criterion) left;
            Criterion valueRight = (Criterion) right;

            // Than calculate the result value
            if (operator == BinaryOperatorKind.AND) {
                return Restrictions.and(valueLeft, valueRight);
            } else {
                // OR
                return Restrictions.or(valueLeft, valueRight);
            }
        } else {
            throw new ODataApplicationException("Boolean operations needs two logical operands",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
    }

    private Object evaluateComparisonOperation(BinaryOperatorKind operator, Object left, Object right) throws ODataApplicationException {
        String propertyName = null;
        Object value = null;

        if (left instanceof EdmProperty) {
            propertyName = ((EdmProperty) left).getName();
            value = right;
        } else if (right instanceof EdmProperty) {
            propertyName = ((EdmProperty) right).getName();
            value = left;
        } else {
            throw new ODataApplicationException("Property/Value is not comparable",
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }

        if (operator == BinaryOperatorKind.EQ) {
            return Restrictions.eq(propertyName, value);
        } else if (operator == BinaryOperatorKind.NE) {
            return Restrictions.ne(propertyName, value);
        } else if (operator == BinaryOperatorKind.GE) {
            return Restrictions.ge(propertyName, value);
        } else if (operator == BinaryOperatorKind.GT) {
            return Restrictions.gt(propertyName, value);
        } else if (operator == BinaryOperatorKind.LE) {
            return Restrictions.le(propertyName, value);
        } else if (operator == BinaryOperatorKind.LT) {
            return Restrictions.lt(propertyName, value);
        } else {
            // this might not be needed.
            throw new ODataApplicationException("Comparison needs two equal types",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
    }

    private Object evaluateArithmeticOperation(BinaryOperatorKind operator, Object left,
                                               Object right) throws ODataApplicationException {

        // First check if the type of both operands is numerical
        if (left instanceof Integer && right instanceof Integer) {
            Integer valueLeft = (Integer) left;
            Integer valueRight = (Integer) right;

            // Than calculate the result value
            if (operator == BinaryOperatorKind.ADD) {
                return valueLeft + valueRight;
            } else if (operator == BinaryOperatorKind.SUB) {
                return valueLeft - valueRight;
            } else if (operator == BinaryOperatorKind.MUL) {
                return valueLeft * valueRight;
            } else if (operator == BinaryOperatorKind.DIV) {
                return valueLeft / valueRight;
            } else {
                // BinaryOperatorKind,MOD
                return valueLeft % valueRight;
            }
        } else {
            throw new ODataApplicationException("Arithmetic operations needs two numeric operands",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
    }

    @Override
    public Object visitUnaryOperator(UnaryOperatorKind unaryOperatorKind, Object o) throws ExpressionVisitException, ODataApplicationException {
        if (unaryOperatorKind == UnaryOperatorKind.NOT && o instanceof Boolean) {
            // 1.) boolean negation
            return !(Boolean) o;
        } else if (unaryOperatorKind == UnaryOperatorKind.MINUS && o instanceof Integer) {
            // 2.) arithmetic minus
            return -(Integer) o;
        }

        // Operation not processed, throw an exception
        throw new ODataApplicationException("Invalid type for unary operator",
                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitMethodCall(MethodKind methodKind, List<Object> list) throws ExpressionVisitException, ODataApplicationException {
        if (methodKind == MethodKind.STARTSWITH) {
            if (list.size() != 2) {
                throw new ODataApplicationException("Invalid Parameters", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
            }

            String propName;
            Object value;

            if (list.get(0) instanceof EdmProperty) {
                propName = ((EdmProperty) list.get(0)).getName();
                value = list.get(1);
            } else {
                value = list.get(0);
                propName = ((EdmProperty) list.get(1)).getName();
            }
            return Restrictions.like(propName, value.toString(), MatchMode.START);
        }else{
            throw new ODataApplicationException("Method is not implemented", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }

    /* THESE METHODS ARE NOT IMPLEMENTED YET!!! */

    @Override
    public Object visitLambdaExpression(String s, String s1, Expression expression) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitAlias(String s) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitTypeLiteral(EdmType edmType) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitLambdaReference(String s) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitEnum(EdmEnumType edmEnumType, List<String> list) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind binaryOperatorKind, Object o, List<Object> list) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }
}
