package ppm.odataprovider.service.excpetion;

public class MoreThanOneEntityFound extends Exception{

    public MoreThanOneEntityFound(String[] params){
        super(String.format("More than one entity found for given values [$s]", String.join(", ",params)));
    }
}
