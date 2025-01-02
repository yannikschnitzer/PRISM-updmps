package explicit;

import prism.ModelType;

public interface L1MDP<Value> extends UMDP<Value>
{
    @Override
    public default ModelType getModelType() {
        return ModelType.L1MDP;
    }

}
