package retrivers;

import enums.CostSensitiveEnum;
import enums.FeatureSelectionEnum;
import enums.FilenamesEnum;
import enums.SamplingEnum;
import model.ClassifierEvaluation;
import org.jetbrains.annotations.NotNull;
import utils.FileUtils;
import view.FileCreator;
import weka.attributeSelection.*;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.ClassBalancer;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;

import java.nio.file.Path;
import java.util.*;

public class WekaInfoRetriever {

    private static final String RANDOM_FOREST = "Random Forest";
    private static final String NAIVE_BAYES = "Naive Bayes";
    private static final String IBK = "IBk";

    private final String projName;
    private final int numIter;

    public WekaInfoRetriever(String projName, int numIter) {
        this.projName = projName;
        this.numIter = numIter;
    }

    public List<ClassifierEvaluation> retrieveClassifiersEvaluation(String projName) throws Exception {

        Map<String, List<ClassifierEvaluation>> classifiersListMap = new HashMap<>();

        classifiersListMap.put(RANDOM_FOREST, new ArrayList<>());

        classifiersListMap.put(IBK, new ArrayList<>());

        classifiersListMap.put(NAIVE_BAYES, new ArrayList<>());

        for(int i=1; i<=this.numIter; i++) {

            Classifier randomForestClassifier = new RandomForest();
            Classifier iBkClassifier = new IBk();
            Classifier naiveBayesClassifier = new NaiveBayes();

            Map<Classifier, String> classifierNameMap = new HashMap<>();

            classifierNameMap.put(randomForestClassifier, RANDOM_FOREST);
            classifierNameMap.put(iBkClassifier, IBK);
            classifierNameMap.put(naiveBayesClassifier, NAIVE_BAYES);

            for(Classifier classifier: classifierNameMap.keySet()) {
                for (FeatureSelectionEnum featureSelectionEnum : FeatureSelectionEnum.values()) {   //Iterate on all feature selection mode
                    for (SamplingEnum samplingEnum : SamplingEnum.values()) {       //Iterate on all sampling mode
                        for (CostSensitiveEnum costSensitiveEnum : CostSensitiveEnum.values()) {    //Iterate on all cost sensitive mode
                            //Evaluate the classifier
                            classifiersListMap.get(classifierNameMap.get(classifier))  //Get the list associated to the actual classifier
                                    .add(useClassifier(i, projName, classifier, classifierNameMap.get(classifier), featureSelectionEnum, samplingEnum, costSensitiveEnum)); //Evaluate the classifier
                       }
                    }
                }
            }
        }

        List<ClassifierEvaluation> classifierEvaluationList = new ArrayList<>();

        for(String classifierName: classifiersListMap.keySet()) {
            classifierEvaluationList.addAll(classifiersListMap.get(classifierName));
        }

        return classifierEvaluationList;
    }

    private @NotNull ClassifierEvaluation useClassifier(int index, String projName, Classifier classifier, String classifierName, @NotNull FeatureSelectionEnum featureSelection, @NotNull SamplingEnum sampling, CostSensitiveEnum costSensitive) throws Exception {

        DataSource source1 = new DataSource(Path.of("retrieved_data", projName, "training", FileUtils.getArffFilename(FilenamesEnum.TRAINING, projName, index)).toString());
        DataSource source2 = new DataSource(Path.of("retrieved_data", projName, "testing",  FileUtils.getArffFilename(FilenamesEnum.TESTING, projName, index)).toString());
        Instances training = source1.getDataSet();
        Instances testing = source2.getDataSet();

        int numAttr = training.numAttributes();
        training.setClassIndex(numAttr - 1);
        testing.setClassIndex(numAttr - 1);

        Evaluation eval = new Evaluation(testing);

        //FEATURE SELECTION
        switch (featureSelection) {
            case GREEDY_BACKWARD_SEARCH -> {
                //FEATURE SELECTION WITH GREEDY BACKWARD SEARCH TECNIQUE
                ASEvaluation subsetEval = new CfsSubsetEval();
                GreedyStepwise search = new GreedyStepwise();
                search.setSearchBackwards(true);

                List<Instances> filteredAttrList = applyFeatureSelection(subsetEval, search, training, testing);

                training = filteredAttrList.get(0);
                testing = filteredAttrList.get(1);
            }
            case BEST_FIRST -> {
                //FEATURE SELECTION WITH BEST FIRST TECNIQUE
                ASEvaluation subsetEval = new CfsSubsetEval();
                BestFirst search = new BestFirst();

                List<Instances> filteredAttrList = applyFeatureSelection(subsetEval, search, training, testing);

                training = filteredAttrList.get(0);
                testing = filteredAttrList.get(1);
            }
        }

        //SAMPLING
        switch (sampling) {
            case UNDERSAMPLING -> {
                //VALIDATION WITH UNDERSAMPLING
                SpreadSubsample spreadSubsample = new SpreadSubsample();
                spreadSubsample.setInputFormat(training);
                spreadSubsample.setOptions(new String[]{"-M", "1.0"});

                FilteredClassifier fc = new FilteredClassifier();
                fc.setFilter(spreadSubsample);
                fc.setClassifier(classifier);

                classifier = fc;
            }
            /*case CLASS_BALANCER -> {
                //VALIDATION WITH CLASS_BALANCER
                ClassBalancer classBalancer = new ClassBalancer();
                classBalancer.setInputFormat(training);
                classBalancer.setOptions(new String[]{"-num-intervals", "10", "-S", "1"});

                FilteredClassifier fc = new FilteredClassifier();
                fc.setFilter(classBalancer);
                fc.setClassifier(classifier);

                classifier = fc;
            }*/
            case OVERSAMPLING -> {
                //VALIDATION WITH OVERSAMPLING
                int[] nominalCounts = training.attributeStats(training.numAttributes() - 1).nominalCounts;
                int numberOfFalse = nominalCounts[1];
                int numberOfTrue = nominalCounts[0];
                double proportionOfMajorityValue = (double) numberOfFalse / (numberOfFalse + numberOfTrue);

                Resample resample = new Resample();
                resample.setInputFormat(training);
                resample.setOptions(new String[]{"-B", "1.0", "-S", "1", "-Z", String.valueOf(proportionOfMajorityValue * 2 * 100)});

                FilteredClassifier fc = new FilteredClassifier();
                fc.setFilter(resample);
                fc.setClassifier(classifier);

                classifier = fc;
            }
        }

        //TODO COST SENSITIVE

        classifier.buildClassifier(training);
        eval.evaluateModel(classifier, testing);
        ClassifierEvaluation simpleRandomForest = new ClassifierEvaluation(this.projName, index, classifierName, featureSelection, sampling, costSensitive);
        simpleRandomForest.setTrainingPercent(100.0 * training.numInstances() / (training.numInstances() + testing.numInstances()));
        simpleRandomForest.setPrecision(eval.precision(0));
        simpleRandomForest.setRecall(eval.recall(0));
        simpleRandomForest.setAuc(eval.areaUnderROC(0));
        simpleRandomForest.setKappa(eval.kappa());
        simpleRandomForest.setTp(eval.numTruePositives(0));
        simpleRandomForest.setFp(eval.numFalsePositives(0));
        simpleRandomForest.setTn(eval.numTrueNegatives(0));
        simpleRandomForest.setFn(eval.numFalseNegatives(0));
        return simpleRandomForest;
    }

    /**
     * This method executes feature selection on the Instances and return a list of two param:
     *      1. the filtered training attribute.
     *      2. the filtered testing attribute.
     * @param training Training Instances on which apply feature selection.
     * @param testing Testing Instances on which apply feature selection.
     * @return A list of two param:
     *      1. the filtered training attribute.
     *      2. the filtered testing attribute.
     */
    private @NotNull List<Instances> applyFeatureSelection(ASEvaluation eval, ASSearch search, Instances training, Instances testing) throws Exception {

        AttributeSelection filter = new AttributeSelection();
        filter.setEvaluator(eval);
        filter.setSearch(search);
        filter.setInputFormat(training);

        Instances filteredTraining = Filter.useFilter(training, filter);
        Instances filteredTesting = Filter.useFilter(testing, filter);

        int numAttrFiltered = filteredTraining.numAttributes();
        filteredTraining.setClassIndex(numAttrFiltered - 1);
        //filteredTesting.setClassIndex(numAttrFiltered - 1);

        List<Instances> filteredAttrList = new ArrayList<>();

        filteredAttrList.add(filteredTraining);
        filteredAttrList.add(filteredTesting);

        return filteredAttrList;
    }

}
